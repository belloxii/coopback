package com.gotryt.coop.dto;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OTPRequest {

    private String email;
    private String otp;
    
    @Column(nullable = false)
    private String action;

}
