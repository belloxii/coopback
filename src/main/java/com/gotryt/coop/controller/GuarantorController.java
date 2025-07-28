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
    public ResponseEntity<?> getGuarantorRequests(@RequestHeader("Authorization") String jwt) {
        User user = userService.findUserProfileByJwt(jwt).getUser();
        List<Loan> loans = loanRepository.findAll().stream()
            .filter(loan ->
                (loan.getGuarantor1() != null && loan.getGuarantor1().getId().equals(user.getId())) ||
                (loan.getGuarantor2() != null && loan.getGuarantor2().getId().equals(user.getId()))
            ).collect(Collectors.toList());

        return ResponseEntity.ok(loans);
    }

    // ✅ 2. Accept guarantor request
    @PutMapping("/accept")
    public ResponseEntity<?> acceptGuarantorRequest(@RequestHeader("Authorization") String jwt, @RequestParam Long loanId, @RequestParam Long userId) {
        return updateGuarantorStatus(loanId, userId, "ACCEPTED");
    }

    // ✅ 3. Decline guarantor request
    @PutMapping("/decline")
    public ResponseEntity<?> declineGuarantorRequest(@RequestHeader("Authorization") String jwt, @RequestParam Long loanId, @RequestParam Long userId) {
        return updateGuarantorStatus(loanId, userId, "DECLINED");
    }

    // ✅ 4. Shared method
    private ResponseEntity<?> updateGuarantorStatus(Long loanId, Long userId, String status) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new RuntimeException("Loan not found"));

        boolean updated = false;

        if (loan.getGuarantor1() != null && loan.getGuarantor1().getId().equals(userId)) {
            loan.setGuarantor1Status(status);
            updated = true;
        } else if (loan.getGuarantor2() != null && loan.getGuarantor2().getId().equals(userId)) {
            loan.setGuarantor2Status(status);
            updated = true;
        }

        if (!updated) {
            return ResponseEntity.badRequest().body("User is not a guarantor for this loan");
        }

        loanRepository.save(loan);
        return ResponseEntity.ok("Guarantor status updated to " + status);
    }
}
