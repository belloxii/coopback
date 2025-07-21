package com.gotryt.coop.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

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

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class RepayServiceImpl implements RepayService {

    private final LoanRepository loanRepository;
    private final RepayRepository repayRepository;
    private final UserRepository userRepository;

@Override
@Transactional
public RepayResponse repayNow(User user, Long loanId, RepayDto repayDto) throws LoanException, RepayException {

    if (user == null) {
        throw new RepayException("User not found");
    }

    Loan loan = loanRepository.findById(loanId)
        .orElseThrow(() -> new LoanException("Loan not found"));

    BigDecimal repaymentAmount = repayDto.getAmount().setScale(2, RoundingMode.HALF_UP);
    BigDecimal repayAmount = loan.getRepayAmount().setScale(2, RoundingMode.HALF_UP);
    BigDecimal currentBalance = loan.getBalance().setScale(2, RoundingMode.HALF_UP);

    if (repaymentAmount.compareTo(BigDecimal.ZERO) <= 0 || repayAmount.compareTo(BigDecimal.ZERO) <= 0) {
        throw new RepayException("Invalid repayment amount or repayment plan.");
    }

    if (repaymentAmount.remainder(repayAmount).compareTo(BigDecimal.ZERO) != 0) {
        throw new RepayException("Repayment must be in multiples of ₦" + repayAmount.toPlainString());
    }

    if (repaymentAmount.compareTo(currentBalance) > 0) {
        throw new RepayException("Repayment amount exceeds outstanding loan balance.");
    }

    BigDecimal remainingBalance = currentBalance.subtract(repaymentAmount).setScale(2, RoundingMode.HALF_UP);

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

    repayRepository.save(repayLoan);
    loan.setBalance(remainingBalance);

    if (remainingBalance.compareTo(BigDecimal.ZERO) == 0) {
        loan.setStatus("completed");
    }

    loanRepository.save(loan);

    BigDecimal updatedLoanBalance = user.getLoanBalance().subtract(repaymentAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    user.setLoanBalance(updatedLoanBalance);
    userRepository.save(user);

    return RepayResponse.builder()
        .responseCode("100")
        .responseMessage("Repayment successful")
        .repay(repayLoan)
        .build();
}

}
