package com.gotryt.coop.service;

import com.gotryt.coop.dto.PaystackInitializeRequest;
import com.gotryt.coop.dto.PaystackInitializeResponse;
import com.gotryt.coop.dto.RepayDto;
import com.gotryt.coop.model.Shares;
import com.gotryt.coop.model.User;
import com.gotryt.coop.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaystackService {

    @Value("${paystack.secret.key}")
    private String paystackSecretKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private final UserRepository userRepository;
    private final SavingService savingService;
    private final RepayService repaymentService;
    private final SharesService sharesService;

    public PaystackInitializeResponse initializeTransaction(PaystackInitializeRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(paystackSecretKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<PaystackInitializeRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<PaystackInitializeResponse> response = restTemplate.postForEntity(
            "https://api.paystack.co/transaction/initialize",
            entity,
            PaystackInitializeResponse.class
        );

        return response.getBody();
    }

    public Map<String, Object> verifyTransaction(String reference) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(paystackSecretKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            "https://api.paystack.co/transaction/verify/" + reference,
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<>() {}
        );

        return response.getBody();
    }

    @Transactional
    public void handleWebhook(Map<String, Object> payload) {
        try {
            String event = (String) payload.get("event");
            if (!"charge.success".equalsIgnoreCase(event)) return;

            Map<String, Object> data = (Map<String, Object>) payload.get("data");
            String reference = (String) data.get("reference");
            String email = (String) ((Map<String, Object>) data.get("customer")).get("email");
            Map<String, Object> metadata = (Map<String, Object>) data.get("metadata");

            // ✅ Verify transaction to be safe
            Map<String, Object> verified = verifyTransaction(reference);
            Boolean verifiedStatus = (Boolean) verified.get("status");
            if (!Boolean.TRUE.equals(verifiedStatus)) {
                log.warn("Paystack verification failed for reference: {}", reference);
                return;
            }

            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                log.warn("User not found for email: {}", email);
                return;
            }

            // ✅ Get transaction amount and type
            BigDecimal amount = new BigDecimal(data.get("amount").toString()).divide(new BigDecimal("100"));
            String type = (String) metadata.get("type");

            switch (type.toLowerCase()) {
                case "savings" -> {
                    savingService.saveNow(user, amount);
                    log.info("✅ Savings processed for user {} (₦{})", user.getId(), amount);
                }
                case "repayment" -> {
                    Long loanId = Long.valueOf(metadata.get("loanId").toString());

                    RepayDto repayDto = RepayDto.builder()
                        .amount(amount)
                        .type("repayment")
                        .loanId(loanId)
                        .build();

                    repaymentService.repayNow(user, loanId, repayDto);
                    log.info("✅ Repayment processed for user {}, loanId={}, amount=₦{}", user.getId(), loanId, amount);
                }
                case "shares" -> {
                    Shares sharesDetails = new Shares();
                    sharesDetails.setAmount(amount);
                    sharesService.addShares(user, sharesDetails);
                    log.info("✅ Shares processed for user {} (₦{})", user.getId(), amount);
                }
                default -> log.warn("❌ Unknown transaction type: {}", type);
            }

        } catch (Exception e) {
            log.error("❌ Error handling Paystack webhook", e);
        }
    }
}
