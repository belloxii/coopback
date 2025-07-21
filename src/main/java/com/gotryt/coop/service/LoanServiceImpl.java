package com.gotryt.coop.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.gotryt.coop.dto.EmailDetails;
import com.gotryt.coop.dto.LoanDto;
import com.gotryt.coop.dto.LoanResponse;
import com.gotryt.coop.exception.LoanException;
import com.gotryt.coop.model.Loan;
import com.gotryt.coop.model.Notification;
import com.gotryt.coop.model.NotificationType;
import com.gotryt.coop.model.User;
import com.gotryt.coop.repository.LoanRepository;
import com.gotryt.coop.repository.NotificationRepository;
import com.gotryt.coop.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class LoanServiceImpl implements LoanService {

    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

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
                .responseMessage("You already applied for a " + loanRequest.getType() + " loan.")
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
                .responseMessage("You cannot combine Material and Real loans at the same time❌.")
                .build();
        }

        // Prevent using same phone number for both guarantors
        if (loanRequest.getGuarantor1().equals(loanRequest.getGuarantor2())) {
            return LoanResponse.builder()
                .responseCode("419")
                .responseMessage("You cannot use the same phone number for both guarantors❌.")
                .build();
        }

        // Prevent user from using self as guarantor
        String applicantPhone = user.getPhone();
        if (applicantPhone.equals(loanRequest.getGuarantor1()) || applicantPhone.equals(loanRequest.getGuarantor2())) {
            return LoanResponse.builder()
                .responseCode("419")
                .responseMessage("You cannot use your own phone number as a guarantor❌.")
                .build();
        }

        // Validate and retrieve guarantors
        User guarantor1 = userRepository.findByPhone(loanRequest.getGuarantor1())
            .orElseThrow(() -> new LoanException("The owner of the phone number " + loanRequest.getGuarantor1() + " is not a cooperator❌."));

        User guarantor2 = userRepository.findByPhone(loanRequest.getGuarantor2())
            .orElseThrow(() -> new LoanException("The owner of the phone number " + loanRequest.getGuarantor2() + " is not a cooperator❌."));

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

        String fullName = 
            (user.getFirstName() != null ? user.getFirstName() + " " : "") +
            (user.getMiddleName() != null ? user.getMiddleName() + " " : "") +
            (user.getLastName() != null ? user.getLastName() : "");

        // Send notifications AFTER loan is saved
        notificationRepository.save(Notification.builder()
            .recipient(guarantor1)
            .type(NotificationType.GUARANTOR_REQUEST)
            .referenceId(applyLoan.getId()) // use loan ID as reference
            .message("You have been selected as a guarantor for a loan by " + fullName)
            .timestamp(LocalDateTime.now())
            .build());

        notificationRepository.save(Notification.builder()
            .recipient(guarantor2)
            .type(NotificationType.GUARANTOR_REQUEST)
            .referenceId(applyLoan.getId()) // use loan ID as reference
            .message("You have been selected as a guarantor for a loan by " + fullName)
            .timestamp(LocalDateTime.now())
            .build());

        return LoanResponse.builder()
            .responseCode("100")
            .responseMessage("Loan application submitted✅.")
            .build();
    }

@Override
public Loan approveLoan(Long loanId) throws LoanException {
    Loan applyLoan = loanRepository.findById(loanId)
        .orElseThrow(() -> new LoanException("Loan not found"));

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

    LocalDate today = LocalDate.now();
    applyLoan.setStatus("approved");
    applyLoan.setStartDate(today);
    applyLoan.setEndDate(today.plusMonths(durationInMonths));

    // Ensure scale of 2
    BigDecimal amount = applyLoan.getAmount().setScale(2, RoundingMode.HALF_UP);
    BigDecimal repayAmount = amount.divide(BigDecimal.valueOf(durationInMonths), 2, RoundingMode.HALF_UP);

    applyLoan.setAmount(amount);
    applyLoan.setBalance(amount);
    applyLoan.setRepayAmount(repayAmount);

    User user = applyLoan.getUser();
    user.setLoanBalance(user.getLoanBalance().add(amount).setScale(2, RoundingMode.HALF_UP));
    userRepository.save(user);

    notificationRepository.save(Notification.builder()
        .recipient(user)
        .type(NotificationType.LOAN_APPROVED)
        .referenceId(applyLoan.getId())
        .message("Your " + applyLoan.getType() + " loan of ₦" + amount + " has been approved.")
        .build());

    emailService.sendEmail(EmailDetails.builder()
        .recipient(user.getEmail())
        .subject("Loan Approved")
        .message("Congratulations, your " + applyLoan.getType() + " loan of ₦" + amount + " has been approved. Login to the app to view more details.")
        .build());

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

        notificationRepository.save(Notification.builder()
            .recipient(applyLoan.getUser())
            .type(NotificationType.LOAN_DECLINED)
            .referenceId(applyLoan.getId())
            .message("Your" + applyLoan.getType() + " loan of ₦" + applyLoan.getAmount() + " has been declined.")
            .build());
    
        EmailDetails emailDetails = EmailDetails.builder()
        .recipient(user.getEmail())
        .subject("Loan Declined")
        .message("This is to notify you that your" + applyLoan.getType() + " loan of ₦" + applyLoan.getAmount() + " was declined. Login to the app to view more details.")
        .build();

        emailService.sendEmail(emailDetails);

        return loanRepository.save(applyLoan);
    }
}
