package com.gotryt.coop.service;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.gotryt.coop.dto.CoopResponse;
import com.gotryt.coop.dto.EmailDetails;
import com.gotryt.coop.dto.OTPRequest;
import com.gotryt.coop.dto.OTPResponse;
import com.gotryt.coop.model.OTP;
import com.gotryt.coop.model.User;
import com.gotryt.coop.repository.OTPRepository;
import com.gotryt.coop.repository.UserRepository;
import com.gotryt.coop.utils.OTPGenerator;

@Service
@RequiredArgsConstructor
public class OTPServiceImpl implements OTPService {

    private final OTPRepository otpRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public CoopResponse sendOTP(OTPRequest otpRequest){
        Optional<User> existingUser = userRepository.findByEmail(otpRequest.getEmail());

        if (existingUser.isPresent()) {
            if ("NEW".equalsIgnoreCase(existingUser.get().getStatus())) {
                return CoopResponse.builder()
                    .responseCode("419")
                    .responseMessage("Your account is pending admin approval.")
                    .build();
            }
            return CoopResponse.builder()
                .responseCode("419")
                .responseMessage("There is already a registered user with this email.")
                .build();
        }

        OTP existingOTP = otpRepository.findByEmail(otpRequest.getEmail());
        if (existingOTP != null) {
            otpRepository.delete(existingOTP);
        }
        
        String otp = OTPGenerator.generateOTP();

        otpRepository.save(OTP.builder()
                .email(otpRequest.getEmail())
                .otp(otp)
                .expiredAt(LocalDateTime.now().plusMinutes(5))
            .build());

        emailService.sendEmail(EmailDetails.builder()
                .subject("Do not disclose!!!")
                .recipient(otpRequest.getEmail())
                .message("Below is your One Time Password to complete your registration. \nThe OTP expires in 5 minutes. \nInput <"+otp+"> to continue in the app")
            .build());

        return CoopResponse.builder()
                .responseCode("100")
                .responseMessage("otp generated and sent successfully")
            .build();
    }

    @Override
    public CoopResponse validateOTP(OTPRequest otpRequest) {

        OTP otp = otpRepository.findByEmail(otpRequest.getEmail());
        if (otp == null) {
            return CoopResponse.builder()
                .responseCode("100")
                .responseMessage("you did not send any OTP")
            .build();
        }

        if (otp.getExpiredAt().isBefore(LocalDateTime.now())) {
            return CoopResponse.builder()
                    .responseCode("419")
                    .responseMessage("OTP expired")
                .build();
        }

        if (!otp.getOtp().equals(otpRequest.getOtp())) {
            
            return CoopResponse.builder()
                    .responseCode("419")
                    .responseMessage("OTP is not correct")
                .build();
        }

        return CoopResponse.builder()
                .responseCode("100")
                .responseMessage("OTP validated successfully")
                .otpResponse(OTPResponse.builder()
                        .isOTPValid(true)
                    .build())
            .build();

    }
   
}
