package com.gotryt.coop.service;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gotryt.coop.dto.RepayDto;
import com.gotryt.coop.dto.RepayResponse;
import com.gotryt.coop.exception.LoanException;
import com.gotryt.coop.exception.RepayException;
import com.gotryt.coop.model.Loan;
import com.gotryt.coop.model.Repay;
import com.gotryt.coop.model.User;
import com.gotryt.coop.repository.LoanRepository;
import com.gotryt.coop.repository.RepayRepository;
import com.gotryt.coop.repository.UserRepository;
import com.gotryt.coop.utils.TxnIdGen;

@Service
public class RepayServiceImpl implements RepayService {

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private RepayRepository repayRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public RepayResponse repayNow(User user, Long loanId, RepayDto repayDto) throws LoanException, RepayException {

        if (user == null) {
            throw new RepayException("User not found");
        }

        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new LoanException("Loan not found"));

        BigDecimal repaymentAmount = repayDto.getAmount();
        BigDecimal repayAmount = loan.getRepayAmount(); // Monthly repayment
        BigDecimal currentBalance = loan.getBalance();

        if (repaymentAmount == null || repayAmount == null || repayAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RepayException("Invalid repayment amount or loan repayment plan.");
        }

        // ✅ Check 1: Repayment must be a multiple of the loan's repayAmount
        if (repaymentAmount.remainder(repayAmount).compareTo(BigDecimal.ZERO) != 0) {
            throw new RepayException("Repayment must be in multiples of ₦" + repayAmount.toPlainString());
        }

        // ✅ Check 2: Repayment must not exceed loan balance
        if (repaymentAmount.compareTo(currentBalance) > 0) {
            throw new RepayException("Repayment amount exceeds outstanding loan balance.");
        }

        // Calculate remaining balance
        BigDecimal remainingBalance = currentBalance.subtract(repaymentAmount);

        // Create repayment transaction
        Repay repayLoan = Repay.builder()
            .amount(repaymentAmount)
            .txnId(TxnIdGen.generateTransactionId())
            .balance(remainingBalance)
            .loan(loan)
            .parentLoanId(loan.getId())
            .loanType(loan.getType())
            .status("approved")
            .user(user)
            .build();

        // Save repayment and update loan
        repayRepository.save(repayLoan);
        loan.setBalance(remainingBalance);

        if (remainingBalance.compareTo(BigDecimal.ZERO) == 0) {
            loan.setStatus("completed");
        }

        loanRepository.save(loan);

        // Update user's overall loan balance
        BigDecimal updatedLoanBalance = user.getLoanBalance().subtract(repaymentAmount);
        user.setLoanBalance(updatedLoanBalance.max(BigDecimal.ZERO));

        userRepository.save(user);

        return RepayResponse.builder()
            .responseCode("100")
            .responseMessage("Repayment successful")
            .repay(repayLoan)
            .build();
    }

}
