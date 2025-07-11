package com.gotryt.coop.controller;

import java.util.Optional;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gotryt.coop.exception.UserException;
import com.gotryt.coop.model.User;
import com.gotryt.coop.repository.UserRepository;
import com.gotryt.coop.service.UserService;

import lombok.AllArgsConstructor;


@RestController
@RequestMapping("/api/user")
@AllArgsConstructor
public class UserController {

    private final UserService userService;

    private final UserRepository userRepository;

    @PutMapping("/update")
    public User updateUser(@RequestBody User user, @RequestHeader("Authorization") String jwt) throws UserException {
        return userService.updateUser(user);
    } 

    @PutMapping("/user/{userId}/activate")
    public User activateUser(@PathVariable Long userId, @RequestHeader("Authorization") String jwt) throws UserException {
        return userService.activateUser(userId);
    }

    @GetMapping("/id/{userId}")
    public Optional<User> findUserById(@PathVariable Long userId, @RequestHeader("Authorization") String jwt) throws UserException {
        return userRepository.findById(userId);
    }

    @GetMapping("/phone/{phone}")
    public Optional<User> findUserByPhone(@PathVariable String phone, @RequestHeader("Authorization") String jwt) throws UserException {
        return userRepository.findByPhone(phone);
    }

}
