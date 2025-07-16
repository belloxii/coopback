package com.gotryt.coop.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gotryt.coop.dto.LoanDto;
import com.gotryt.coop.dto.LoanResponse;
import com.gotryt.coop.exception.LoanException;
import com.gotryt.coop.model.Loan;
import com.gotryt.coop.model.User;
import com.gotryt.coop.repository.LoanRepository;
import com.gotryt.coop.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class LoanServiceImpl implements LoanService {

    @Autowired
    LoanRepository loanRepository;

    @Autowired
    UserRepository userRepository;

    @Override
    @Transactional
    public LoanResponse applyLoan(User user, LoanDto loanRequest) throws LoanException {
        
        // Check for existing same type loan
        Optional<Loan> existingSameTypeLoan = loanRepository.findFirstByUserAndTypeAndStatusIn(
            user,
            loanRequest.getType(),
            List.of("approved", "submitted")
        );

        if (existingSameTypeLoan.isPresent()) {
            return LoanResponse.builder()
                .responseCode("419")
                .responseMessage("You already have an active " + loanRequest.getType() + " loan.")
                .build();
        }

        // Check if combining real + material
        List<Loan> activeLoans = loanRepository.findByUserAndStatusIn(user, List.of("approved", "submitted"));
        Set<String> activeLoanTypes = activeLoans.stream()
            .map(l -> l.getType().toLowerCase())
            .collect(Collectors.toSet());

        String newLoanType = loanRequest.getType().toLowerCase();
        if ((activeLoanTypes.contains("real") && newLoanType.equals("material")) ||
            (activeLoanTypes.contains("material") && newLoanType.equals("real"))) {
            return LoanResponse.builder()
                .responseCode("419")
                .responseMessage("You cannot combine Material and Real loans at the same time.")
                .build();
        }

        // Prevent using same phone number for both guarantors
        if (loanRequest.getGuarantor1().equals(loanRequest.getGuarantor2())) {
            return LoanResponse.builder()
                .responseCode("419")
                .responseMessage("You cannot use the same phone number for both guarantors.")
                .build();
        }

        // Prevent user from using self as guarantor
        String applicantPhone = user.getPhone();
        if (applicantPhone.equals(loanRequest.getGuarantor1()) || applicantPhone.equals(loanRequest.getGuarantor2())) {
            return LoanResponse.builder()
                .responseCode("419")
                .responseMessage("You cannot use your own phone number as a guarantor.")
                .build();
        }

        // Validate and retrieve guarantor 1
        User guarantor1 = userRepository.findByPhone(loanRequest.getGuarantor1())
            .orElseThrow(() -> new LoanException("The owner of the phone number " + loanRequest.getGuarantor1() + " is not a cooperator."));

        // Validate and retrieve guarantor 2
        User guarantor2 = userRepository.findByPhone(loanRequest.getGuarantor2())
            .orElseThrow(() -> new LoanException("The owner of the phone number " + loanRequest.getGuarantor2() + " is not a cooperator."));

        // Create loan
        Loan applyLoan = Loan.builder()
            .amount(loanRequest.getAmount())
            .balance(BigDecimal.ZERO)
            .type(loanRequest.getType())
            .duration(loanRequest.getDuration())
            .user(user)
            .accountDetails(loanRequest.getAccountDetails())
            .purpose(loanRequest.getPurpose())
            .status("submitted")
            .guarantor1(guarantor1)
            .guarantor2(guarantor2)
            .build();

        loanRepository.save(applyLoan);

        return LoanResponse.builder()
            .responseCode("100")
            .responseMessage("Loan application submitted.")
            .build();
    }

   @Override
    public Loan approveLoan(Long loanId) throws LoanException {
        // Fetch loan
        Loan applyLoan = loanRepository.findById(loanId)
            .orElseThrow(() -> new LoanException("Loan not found"));

        // Validate and parse duration
        String durationStr = applyLoan.getDuration();
        if (durationStr == null || durationStr.trim().isEmpty()) {
            throw new LoanException("Loan duration is missing or invalid.");
        }

        long durationInMonths;
        try {
            durationInMonths = Long.parseLong(durationStr.trim());
        } catch (NumberFormatException e) {
            throw new LoanException("Loan duration must be a valid numeric value.");
        }

        if (durationInMonths <= 0) {
            throw new LoanException("Loan duration must be greater than zero.");
        }

        // Set loan status and dates
        LocalDate todaysDate = LocalDate.now();
        applyLoan.setStatus("approved");
        applyLoan.setBalance(applyLoan.getAmount());
        applyLoan.setStartDate(todaysDate);
        applyLoan.setEndDate(todaysDate.plusMonths(durationInMonths));

        // Calculate monthly repayment amount
        BigDecimal repayAmount = applyLoan.getAmount().divide(
            BigDecimal.valueOf(durationInMonths), RoundingMode.HALF_UP
        );
        applyLoan.setRepayAmount(repayAmount);

        // Update user loan balance
        User user = applyLoan.getUser();
        user.setLoanBalance(user.getLoanBalance().add(applyLoan.getAmount()));
        userRepository.save(user);

        // Save and return updated loan
        return loanRepository.save(applyLoan);
    }

    @Override
    public Loan rejectLoan(Long loanId, LoanDto loanDto) throws LoanException {
        Loan applyLoan = loanRepository.findById(loanId)
            .orElseThrow(() -> new LoanException("Loan not found"));

        // Set loan details
        applyLoan.setStatus("declined");
        applyLoan.setRemark(loanDto.getRemark());

   
        User user = applyLoan.getUser();
        user.setLoanBalance(user.getLoanBalance().add(applyLoan.getAmount()));
        userRepository.save(user);

        return loanRepository.save(applyLoan);
    }
}
