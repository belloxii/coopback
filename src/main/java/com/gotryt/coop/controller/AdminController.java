package com.gotryt.coop.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gotryt.coop.dto.AuthResponse;
import com.gotryt.coop.dto.LoanDto;
import com.gotryt.coop.dto.UserRequest;
import com.gotryt.coop.exception.LoanException;
import com.gotryt.coop.exception.SavingException;
import com.gotryt.coop.exception.SharesException;
import com.gotryt.coop.exception.UserException;
import com.gotryt.coop.model.Loan;
import com.gotryt.coop.model.Notification;
import com.gotryt.coop.model.Repay;
import com.gotryt.coop.model.Saving;
import com.gotryt.coop.model.Shares;
import com.gotryt.coop.model.User;
import com.gotryt.coop.repository.LoanRepository;
import com.gotryt.coop.repository.NotificationRepository;
import com.gotryt.coop.repository.RepayRepository;
import com.gotryt.coop.repository.SavingRepository;
import com.gotryt.coop.repository.SharesRepository;
import com.gotryt.coop.repository.UserRepository;
import com.gotryt.coop.service.LoanService;
import com.gotryt.coop.service.UserService;

import lombok.AllArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/admin")
@AllArgsConstructor
public class AdminController {

    private final UserService userService;
    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final LoanService loanService;
    private final RepayRepository repayRepository;
    private final NotificationRepository notificationRepository;
    private final SavingRepository savingRepository;
    private final SharesRepository sharesRepository;

    @GetMapping("/users")
    public List<User> allUsers(@RequestHeader("Authorization") String jwt) {
        return userRepository.findAll();
    }

    @GetMapping("/user/id/{userId}")
    public Optional<User> findUserById(@PathVariable Long userId, @RequestHeader("Authorization") String jwt) throws UserException {
        return userRepository.findById(userId);
    }

    @GetMapping("/user/memship/{memship}")
    public User findUserByMemship(@PathVariable String memship, @RequestHeader("Authorization") String jwt) throws UserException {
        return userRepository.findByMemship(memship);
    }

    @GetMapping("/users/new")
    public List<User> findNewUsers(@RequestHeader("Authorization") String jwt) throws UserException {
        return userRepository.findByStatus("new");
    } 

    @PutMapping("/user/{userId}/activate")
    public User activateUser(@PathVariable Long userId, @RequestHeader("Authorization") String jwt) throws UserException {
        return userService.activateUser(userId);
    }

    @PutMapping("/user/update")
    public User updateUser(@RequestBody User user, @RequestHeader("Authorization") String jwt) throws UserException {
        return userService.updateUser(user);
    } 

    @PostMapping("/users/multi")
    public List<AuthResponse> addMultiUsers(@RequestBody List<UserRequest> users) throws UserException {
        return userService.addMultiUsers(users);
    }

    @GetMapping("/loan/loansbyuserid/{userId}")
    public List<Loan> loansByUserId(@RequestHeader("Authorization") String jwt, @PathVariable Long userId) throws LoanException {
        return loanRepository.findByUser_Id(userId);
    }

    @GetMapping("/loan/all")
    public List<Loan> allLoans(@RequestHeader("Authorization") String jwt) throws LoanException {
        return loanRepository.findAll();
    }

    @PutMapping("/loan/{loanId}/approve")
    public Loan approveLoan(@RequestHeader("Authorization") String jwt, @PathVariable Long loanId) throws LoanException {
        return loanService.approveLoan(loanId);
    }

    @PutMapping("/loan/{loanId}/reject")
    public Loan rejectLoan(@RequestHeader("Authorization") String jwt, @PathVariable Long loanId, @RequestBody LoanDto loanDto) throws LoanException {
        return loanService.rejectLoan(loanId, loanDto);
    }

    @GetMapping("/repays/all")
    public List<Repay> allRepay(@RequestHeader("Authorization") String jwt) {
        return repayRepository.findAll();
    }

    @GetMapping("/repays/user/{userId}")
    public List<Repay> userRepay(@RequestHeader("Authorization") String jwt, @PathVariable Long userId) {
        return repayRepository.findByUser_Id(userId);
    }

    // Admin or Debug: Get all notifications
    @GetMapping("/notis/all")
    public ResponseEntity<List<Notification>> getAllNotifications() {
        return ResponseEntity.ok(notificationRepository.findAll());
    }

    @GetMapping("/savings/all")
    public List<Saving> allSavings(@RequestHeader("Authorization") String jwt) throws SavingException {
        return savingRepository.findAll();
    }

    @GetMapping("/savings/user/{userId}")
    public List<Saving> savingByUserId(@RequestHeader("Authorization") String jwt, @PathVariable Long userId) throws SavingException {
        return savingRepository.findByUser_Id(userId);
    }

    @GetMapping("/shares/all")
    public ResponseEntity<List<Shares>> getAllShares(@RequestHeader("Authorization") String jwt) throws SharesException {
        return ResponseEntity.ok(sharesRepository.findAll());
    }
    
}
