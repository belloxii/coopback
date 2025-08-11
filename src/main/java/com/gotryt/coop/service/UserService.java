package com.gotryt.coop.service;

import java.util.List;

import com.gotryt.coop.dto.AuthResponse;
import com.gotryt.coop.dto.CoopResponse;
import com.gotryt.coop.dto.LoginDto;
import com.gotryt.coop.dto.PasswordDto;
import com.gotryt.coop.dto.UserRequest;
import com.gotryt.coop.exception.UserException;
import com.gotryt.coop.model.User;

public interface UserService {

    AuthResponse createAccount(UserRequest userRequest);
    AuthResponse login(LoginDto loginDto);
    AuthResponse findUserProfileByJwt(String jwt);    
    // User findUserById(Long userId) throws UserException;    
    public List<AuthResponse> addMultiUsers(List<UserRequest> requests) throws UserException;
    public User updateUser(User user) throws UserException;
    public User activateUser(Long userId) throws UserException;
    public CoopResponse changePassword(User user, PasswordDto passwordDto) throws UserException;

}
