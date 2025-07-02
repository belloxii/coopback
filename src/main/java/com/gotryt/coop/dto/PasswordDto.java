package com.gotryt.coop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PasswordDto {

    private String oldPassword;
    private String newPassword;
    private String confirmPassword;
    private String otp;

}
