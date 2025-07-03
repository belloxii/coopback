package com.gotryt.coop.service;

import com.gotryt.coop.dto.UserRequest;
import com.gotryt.coop.exception.UserException;
import com.gotryt.coop.model.OTP;
import com.gotryt.coop.model.Role;
import com.gotryt.coop.model.User;
import com.gotryt.coop.repository.OTPRepository;
import com.gotryt.coop.repository.UserRepository;
import com.gotryt.coop.utils.MemshipGen;

import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.gotryt.coop.config.JwtProvider;
import com.gotryt.coop.dto.AuthResponse;
import com.gotryt.coop.dto.CoopResponse;
import com.gotryt.coop.dto.EmailDetails;
import com.gotryt.coop.dto.LoginDto;
import com.gotryt.coop.dto.PasswordDto;


@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final OTPRepository otpRepository;

  @Override
public AuthResponse createAccount(UserRequest userRequest) {

    // Check if user with email already exists
    Optional<User> existingUser = userRepository.findByEmail(userRequest.getEmail());
    if (existingUser.isPresent()) {
        if ("NEW".equalsIgnoreCase(existingUser.get().getStatus())) {
            return AuthResponse.builder()
                .responseCode("419")
                .responseMessage("Your account has been created, please wait for the admin to review and activate your account!")
                .jwt(null)
                .build();
        }
        return AuthResponse.builder()
            .responseCode("419")
            .responseMessage("Email already exists!")
            .jwt(null)
            .build();
    }

    // Check for duplicate memship ID if it comes from the client (you can skip this if memship is generated)
    if (userRequest.getMemship() != null && userRepository.existsByMemship(userRequest.getMemship())) {
        return AuthResponse.builder()
            .responseCode("419")
            .responseMessage("Membership ID already exists!")
            .jwt(null)
            .build();
    }

    // Generate ledger number and memship
    Integer lastLedgerNumber = userRepository.findMaxLedgerNumber();
    int nextLedgerNumber = (lastLedgerNumber == null) ? 1 : lastLedgerNumber + 1;
    String memship = MemshipGen.genMemship(nextLedgerNumber);

    // Create user entity
    User newUser = User.builder()
        .firstName(userRequest.getFirstName())
        .lastName(userRequest.getLastName())
        .middleName(userRequest.getMiddleName())
        .gender(userRequest.getGender())
        .address(userRequest.getAddress())
        .state(userRequest.getState())
        .phone(userRequest.getPhone())
        .phone2(userRequest.getPhone2())
        .passport(userRequest.getPassport())
        .email(userRequest.getEmail())
        .dob(userRequest.getDob())
        .marital(userRequest.getMarital())
        .psn(userRequest.getPsn())
        .lga(userRequest.getLga())
        .station(userRequest.getStation())
        .password(passwordEncoder.encode("123456")) // default password
        .memship(memship)
        .ledgerNumber(nextLedgerNumber)
        .savingsBalance(BigDecimal.ZERO)
        .loanBalance(BigDecimal.ZERO)
        .sharesBalance(BigDecimal.ZERO)
        .totalPurchase(BigDecimal.ZERO)
        .plan(userRequest.getPlan())
        .status("NEW")
        .role(Role.MEMBER)
        .build();

    // Save to database
    User createdUser = userRepository.save(newUser);

    String names = createdUser.getFirstName() + " " + createdUser.getLastName() + " " + createdUser.getMiddleName();

    // Send welcome email
    EmailDetails emailDetails = EmailDetails.builder()
        .recipient(createdUser.getEmail())
        .subject("New Account Creation")
        .message("Congratulations, your COOPR8 account has been created.\n\n"
                + "Account Name: " + names
                + "\nMembership Number: " + createdUser.getMemship()
                + "\nPassword: 123456\n\nYou will be notified via email once an admin reviews your profile then you will be able to log in.")
        .build();

    emailService.sendEmail(emailDetails);

    return AuthResponse.builder()
        .responseCode("100")
        .responseMessage("Account created successfully! Kindly check your email for more info.")
        .build();
}

    @Override
    public AuthResponse login(LoginDto loginDto) {
    
        String memship = loginDto.getMemship();
        User user = userRepository.findByMemship(memship);
        
        if (userRepository.existsByMemship(loginDto.getMemship()) 
            && "NEW".equalsIgnoreCase(user.getStatus())) {
                
                return AuthResponse.builder()
                    .responseCode("419")
                    .responseMessage("Your account has been created, please wait for the admin to review and activate your account!\n\nYou will be notified via email once your account is activated❤️.")
                    .jwt(null)
                    .build();
            }
    
        if (user == null) {
            return AuthResponse.builder()
                .responseCode("419")
                .responseMessage("No user found")
                .jwt(null)
                .build();
        }

        // Use matches() for proper password verification
        if (!passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
            return AuthResponse.builder()
                .responseCode("419")
                .responseMessage("Invalid password")
                .jwt(null)
                .build();
        }
    
        Authentication authentication = new UsernamePasswordAuthenticationToken(loginDto.getMemship(), loginDto.getPassword());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    
        String jwt = jwtProvider.generateToken(authentication);
    
        return AuthResponse.builder()
                .responseCode("100")
                .responseMessage("Login success")
                .jwt(jwt)
                .build();
    }
    
    @Override
    public AuthResponse findUserProfileByJwt(String jwt)  {

        String memship = jwtProvider.getMemshipFromToken(jwt);
        User user = userRepository.findByMemship(memship);

        if (user == null) {
            return AuthResponse.builder()
                .responseCode("419")
                .responseMessage("No user found")
                .jwt(null)
                // .user(user)
            .build();
        }

        return AuthResponse.builder()
                .responseCode("100")
                .responseMessage("user found with jwt")
                .jwt(jwt)
                .user(user)
            .build();
    }

    @Override
    public User findUserById(Long userId) throws UserException {
        User user = userRepository.findById(userId).orElseThrow(
            () -> new UserException("use not found with id: " + userId)
        );
        return user;
    }

    @Override
    public User updateUser(User userData) throws UserException {
        
        // Retrieve the existing user or throw an exception if not found
        User user = userRepository.findById(userData.getId())
                .orElseThrow(() -> new UserException("User not found"));

        if (userData.getFirstName() != null) {
            user.setFirstName(userData.getFirstName());
        }
        if (userData.getMiddleName() != null) {
            user.setMiddleName(userData.getMiddleName());
        }
        if (userData.getLastName() != null) {
            user.setLastName(userData.getLastName());
        }
        if (userData.getGender() != null) {
            user.setGender(userData.getGender());
        }
        if (userData.getEmail() != null) {
            user.setEmail(userData.getEmail());
        }
        if (userData.getPassport() != null) {
            user.setPassport(userData.getPassport());
        }
        if (userData.getAddress() != null) {
            user.setAddress(userData.getAddress());
        }
        if (userData.getState() != null) {
            user.setState(userData.getState());
        }
        if (userData.getPassword() != null) {
            // Encode the new password before setting it
            user.setPassword(passwordEncoder.encode(userData.getPassword()));
        }
        if (userData.getPsn() != null) {
            user.setPsn(userData.getPsn());
        }
        if (userData.getMemship() != null) {
            user.setMemship(userData.getMemship());
        }
        if (userData.getPhone() != null) {
            user.setPhone(userData.getPhone());
        }
        if (userData.getPhone2() != null) {
            user.setPhone2(userData.getPhone2());
        }
        if (userData.getDob() != null) {
            user.setDob(userData.getDob());
        }
        if (userData.getStation() != null) {
            user.setStation(userData.getStation());
        }
        if (userData.getStatus() != null) {
            user.setStatus(userData.getStatus());
        }
        if (userData.getMarital() != null) {
            user.setMarital(userData.getMarital());
        }
        if (userData.getLga() != null) {
            user.setLga(userData.getLga());
        }
        if (userData.getPlan() != null) {
            user.setPlan(userData.getPlan());
        }

        return userRepository.save(user);
    }

    @Override
    public CoopResponse changePassword(User user, PasswordDto passwordDto) {
        CoopResponse response = new CoopResponse();

        if (passwordDto.getOtp() == null || passwordDto.getOtp().isBlank()) {
            return CoopResponse.builder()
                .responseCode("419")
                .responseMessage("invalid OTP detected!")
            .build();
        }

        OTP otp = otpRepository.findByEmail(user.getEmail());
        if (otp == null) {
            return CoopResponse.builder()
                .responseCode("419")
                .responseMessage("you did not send any OTP")
            .build();
        }

        if (!otp.getOtp().equals(passwordDto.getOtp())) {   
            return CoopResponse.builder()
                .responseCode("419")
                .responseMessage("OTP is not correct")
            .build();
        }

        if (otp.getExpiredAt().isBefore(LocalDateTime.now())) {
            return CoopResponse.builder()
                .responseCode("419")
                .responseMessage("OTP expired")
            .build();
        }

        // 1. Validate old password
        if (!passwordEncoder.matches(passwordDto.getOldPassword(), user.getPassword())) {
            response.setResponseCode("419");
            response.setResponseMessage("Old password is incorrect");
            return response;
        }

        // 2. Check if new password matches confirmation
        if (!passwordDto.getNewPassword().equals(passwordDto.getConfirmPassword())) {
            response.setResponseCode("419");
            response.setResponseMessage("New password and confirmation password do not match");
            return response;
        }

        // 3. Check if new password is same as old password
        if (passwordEncoder.matches(passwordDto.getNewPassword(), user.getPassword())) {
            response.setResponseCode("419");
            response.setResponseMessage("New password cannot be the same as the old password");
            return response;
        }

        // 3. Validate password strength
        String newPassword = passwordDto.getNewPassword();
        String passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
        if (!newPassword.matches(passwordRegex)) {
            return CoopResponse.builder()
                .responseCode("419")
                .responseMessage("Password must be at least 8 characters long and include uppercase, lowercase, number, and special character")
                .build();
        }

        // 4. Encode and save new password
        user.setPassword(passwordEncoder.encode(passwordDto.getNewPassword()));
        userRepository.save(user);

        // 5. Return success
        response.setResponseCode("100");
        response.setResponseMessage("Password changed successfully");
        return response;
    }

    @Override
    public User activateUser(Long userId) throws UserException {

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserException("User not found"));

    String names = user.getFirstName() + " " + user.getLastName() + " " + user.getMiddleName();

    // Send welcome email
    EmailDetails emailDetails = EmailDetails.builder()
        .recipient(user.getEmail())
        .subject("Account Activated")
        .message("Congratulations, your account COOPR8 has been activated.\n\n"
                + "Account Name: " + names
                + "\nMembership Number: " + user.getMemship()
                + "\nPassword: 123456\n\nYou can now login to your COOPR8 account with your Membership Number and password.")
        .build();

    emailService.sendEmail(emailDetails);

        user.setStatus("ACTIVE");

        return userRepository.save(user);
    }

@Override
public List<User> addMultiUsers(List<User> users) throws UserException {
    List<User> newUsers = users.stream()
        .map(u -> {
            User user = new User();
            user.setFirstName(u.getFirstName());
            user.setLastName(u.getLastName());
            user.setEmail(u.getEmail());
            user.setPhone(u.getPhone());
            user.setMemship(u.getMemship());
            user.setStatus("new"); // default status
            user.setRole(Role.MEMBER);  // default role
            user.setCreatedAt(LocalDateTime.now());
            return user;
        })
        .collect(Collectors.toList());

    return userRepository.saveAll(newUsers);
}

}
