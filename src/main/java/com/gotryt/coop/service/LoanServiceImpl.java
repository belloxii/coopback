package com.gotryt.coop.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gotryt.coop.dto.LoanDto;
import com.gotryt.coop.dto.LoanResponse;
import com.gotryt.coop.exception.LoanException;
import com.gotryt.coop.model.Loan;
import com.gotryt.coop.model.User;
import com.gotryt.coop.repository.LoanRepository;
import com.gotryt.coop.repository.UserRepository;

@Service
public class LoanServiceImpl implements LoanService {

    @Autowired
    LoanRepository loanRepository;

    @Autowired
    UserRepository userRepository;

    @Override
    public LoanResponse applyLoan(User user, LoanDto loanRequest) throws LoanException {
        
        // Check if user has an outstanding loan balance
        Optional<Loan> existingUnpaidLoan = loanRepository.findFirstByUserAndTypeAndStatusIn(
                user,
                loanRequest.getType(),
                List.of("approved", "submitted") // or any status that indicates it's not completed/cleared
            );

            if (existingUnpaidLoan.isPresent() && existingUnpaidLoan.get().getBalance().compareTo(BigDecimal.ZERO) > 0) {
                return LoanResponse.builder()
                    .responseCode("419")
                    .responseMessage("You have an outstanding " + loanRequest.getType() + " loan that must be repaid before applying again.")
                    .build();
            }

    
        // Create new loan application
        Loan applyLoan = Loan.builder()
            .amount(loanRequest.getAmount())
            .balance(BigDecimal.ZERO)
            .type(loanRequest.getType())
            .duration(loanRequest.getDuration())
            .user(user)
            .bankName(loanRequest.getBankName())
            .aza(loanRequest.getAza())
            .accountName(loanRequest.getAccountName())
            .purpose(loanRequest.getPurpose())
            .status("submitted")
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
