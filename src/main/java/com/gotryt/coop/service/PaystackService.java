package com.gotryt.coop.service;

import com.gotryt.coop.dto.PaystackInitializeRequest;
import com.gotryt.coop.dto.PaystackInitializeResponse;
import com.gotryt.coop.dto.RepayDto;
import com.gotryt.coop.exception.LoanException;
import com.gotryt.coop.exception.RepayException;
import com.gotryt.coop.exception.SharesException;
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
            // Step 1: Validate event type
            String event = (String) payload.get("event");
            if (!"charge.success".equalsIgnoreCase(event)) {
                log.warn("Skipping event: {}", event);
                return;
            }

            // Step 2: Validate and extract data object
            Object dataObj = payload.get("data");
            if (!(dataObj instanceof Map<?, ?> data)) {
                log.warn("Invalid 'data' object format in webhook");
                return;
            }

            // Step 3: Extract important fields
            String reference = (String) data.get("reference");
            Object customerObj = data.get("customer");
            Object metadataObj = data.get("metadata");

            if (!(customerObj instanceof Map<?, ?> customer)) {
                log.warn("Missing or invalid 'customer' object");
                return;
            }
            if (!(metadataObj instanceof Map<?, ?> metadata)) {
                log.warn("Missing or invalid 'metadata' object");
                return;
            }

            String email = (String) customer.get("email");
            if (email == null) {
                log.warn("Customer email is null in webhook data");
                return;
            }

            // Step 4: Re-verify transaction
            Map<String, Object> verified = verifyTransaction(reference);
            if (!Boolean.TRUE.equals(verified.get("status"))) {
                log.warn("Paystack verification failed for reference: {}", reference);
                return;
            }

            // Step 5: Get user by email
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                log.warn("No user found with email: {}", email);
                return;
            }

            // Step 6: Get amount and type
            BigDecimal amount = new BigDecimal(data.get("amount").toString()).divide(BigDecimal.valueOf(100));
            String type = (String) metadata.get("type");

            if (type == null) {
                log.warn("Transaction type is missing in metadata for user {}", user.getId());
                return;
            }

            // Step 7: Handle by type
            switch (type.toLowerCase()) {
                case "savings" -> handleSavings(user, amount);
                case "repayment" -> handleRepayment(user, metadata, amount);
                case "shares" -> handleShares(user, amount);
                default -> log.warn("❌ Unknown transaction type '{}' for user {}", type, user.getId());
            }

        } catch (Exception e) {
            log.error("❌ Exception while handling Paystack webhook", e);
        }
    }

    private void handleSavings(User user, BigDecimal amount) {
        if (user.getPlan() == null) {
            log.warn("User {} has no plan set for savings", user.getId());
            return;
        }

        if (amount.compareTo(user.getPlan()) == 0) {
            savingService.saveNow(user);
            log.info("✅ Savings processed for user {} (₦{})", user.getId(), amount);
        } else {
            log.warn("❌ Amount mismatch: paid ₦{}, expected ₦{} for user {}", amount, user.getPlan(), user.getId());
        }
    }

    private void handleRepayment(User user, Map<?, ?> metadata, BigDecimal amount) throws LoanException, RepayException {
        Object loanIdObj = metadata.get("loanId");
        if (loanIdObj == null) {
            log.warn("❌ Missing loanId in repayment metadata for user {}", user.getId());
            return;
        }

        try {
            Long loanId = Long.parseLong(loanIdObj.toString());
            RepayDto repayDto = RepayDto.builder()
                .amount(amount)
                .type("repayment")
                .loanId(loanId)
                .build();

            repaymentService.repayNow(user, loanId, repayDto);
            log.info("✅ Repayment processed for user {}, loanId={}, amount=₦{}", user.getId(), loanId, amount);

        } catch (NumberFormatException e) {
            log.warn("Invalid loanId format: {} for user {}", loanIdObj, user.getId());
        }
    }

    private void handleShares(User user, BigDecimal amount) throws SharesException {
        Shares sharesDetails = new Shares();
        sharesDetails.setAmount(amount);
        sharesService.addShares(user, sharesDetails);
        log.info("✅ Shares processed for user {} (₦{})", user.getId(), amount);
    }
}
