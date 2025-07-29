package com.gotryt.coop.controller;

import com.gotryt.coop.model.Loan;
import com.gotryt.coop.model.User;
import com.gotryt.coop.repository.LoanRepository;
import com.gotryt.coop.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/guarantor")
@RequiredArgsConstructor
public class GuarantorController {

    private final LoanRepository loanRepository;
    private final UserService userService;

    // ✅ 1. Get all loans where user is a guarantor
    @GetMapping("/myrequests")
    public List<Loan> getGuarantorRequests(@RequestHeader("Authorization") String jwt) {
        User user = userService.findUserProfileByJwt(jwt).getUser();
        return loanRepository.findAll().stream()
            .filter(loan ->
                (loan.getGuarantor1() != null && loan.getGuarantor1().getId().equals(user.getId())) ||
                (loan.getGuarantor2() != null && loan.getGuarantor2().getId().equals(user.getId()))
            ).collect(Collectors.toList());
    }

    // ✅ 2. Accept guarantor request - separate method
    @PutMapping("/accept")
    public ResponseEntity<?> acceptGuarantor(@RequestHeader("Authorization") String jwt, @RequestParam Long loanId) {
        User user = userService.findUserProfileByJwt(jwt).getUser();
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new RuntimeException("Loan not found"));

        boolean isGuarantor = false;

        if (loan.getGuarantor1() != null && loan.getGuarantor1().getId().equals(user.getId())) {
            loan.setGuarantor1Status("ACCEPTED");
            isGuarantor = true;
        } else if (loan.getGuarantor2() != null && loan.getGuarantor2().getId().equals(user.getId())) {
            loan.setGuarantor2Status("ACCEPTED");
            isGuarantor = true;
        }

        if (!isGuarantor) {
            return ResponseEntity.badRequest().body("User is not a guarantor for this loan");
        }

        loanRepository.save(loan);
        return ResponseEntity.ok("Guarantor request accepted.");
    }

    // ✅ 3. Decline guarantor request - separate method
    @PutMapping("/decline")
    public ResponseEntity<?> declineGuarantor(@RequestHeader("Authorization") String jwt, @RequestParam Long loanId) {
        User user = userService.findUserProfileByJwt(jwt).getUser();
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new RuntimeException("Loan not found"));

        boolean isGuarantor = false;

        if (loan.getGuarantor1() != null && loan.getGuarantor1().getId().equals(user.getId())) {
            loan.setGuarantor1Status("DECLINED");
            isGuarantor = true;
        } else if (loan.getGuarantor2() != null && loan.getGuarantor2().getId().equals(user.getId())) {
            loan.setGuarantor2Status("DECLINED");
            isGuarantor = true;
        }

        if (!isGuarantor) {
            return ResponseEntity.badRequest().body("User is not a guarantor for this loan");
        }

        loanRepository.save(loan);
        return ResponseEntity.ok("Guarantor request declined.");
    }
}
