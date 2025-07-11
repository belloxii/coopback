package com.gotryt.coop.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gotryt.coop.dto.AuthResponse;
import com.gotryt.coop.dto.CoopResponse;
import com.gotryt.coop.dto.LoginDto;
import com.gotryt.coop.dto.PasswordDto;
import com.gotryt.coop.dto.UserRequest;
import com.gotryt.coop.exception.UserException;
import com.gotryt.coop.model.User;
import com.gotryt.coop.service.UserService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;


@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/signup")
    public AuthResponse createAccount(@RequestBody UserRequest userRequest) {
        return userService.createAccount(userRequest);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginDto loginDto) {
        return userService.login(loginDto);
    }

    @GetMapping("/profile")
    public AuthResponse getUserProfile(@RequestHeader("Authorization") String jwt) {                          
        return userService.findUserProfileByJwt(jwt);
    }

    @PutMapping("/changepass")
    public CoopResponse changePass(@Valid @RequestHeader("Authorization") String jwt, @RequestBody PasswordDto passwordDto) throws UserException {   
        User user = userService.findUserProfileByJwt(jwt).getUser();                       
        return userService.changePassword(user, passwordDto);
    }
    
}
