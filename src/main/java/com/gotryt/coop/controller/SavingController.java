package com.gotryt.coop.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gotryt.coop.exception.SavingException;
import com.gotryt.coop.model.Saving;
import com.gotryt.coop.model.User;
import com.gotryt.coop.repository.SavingRepository;
import com.gotryt.coop.service.UserService;

import lombok.AllArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@RestController
@RequestMapping("/api/savings")
@AllArgsConstructor
public class SavingController {

    private final UserService userService;

    private final SavingRepository savingRepository;
    
    // @PostMapping("/save")
    // public Saving savevNow(@RequestHeader("Authorization") String jwt) throws SavingException {
    //     User user = userService.findUserProfileByJwt(jwt).getUser();
    //     return savingService.saveNow(user);
    // }

    // @GetMapping("/all")
    // public List<Saving> allsaSavings(@RequestHeader("Authorization") String jwt) throws SavingException {
    //     return savingRepository.findAll();
    // }

    @GetMapping("/mysavings")
    public List<Saving> mySavings(@RequestHeader("Authorization") String jwt) throws SavingException {
        User user = userService.findUserProfileByJwt(jwt).getUser();
        return savingRepository.findByUser_Id(user.getId());
    }

    @GetMapping("/{savingId}")
    public Optional<Saving> savingById(@RequestHeader("Authorization") String jwt, @PathVariable Long savingId) throws SavingException {
        return savingRepository.findById(savingId);
    }

    // @GetMapping("/user/{userId}")
    // public List<Saving> savingByUserId(@RequestHeader("Authorization") String jwt, @PathVariable Long userId) throws SavingException {
    //     return savingRepository.findByUser_Id(userId);
    // }

    @GetMapping("/last")
    public Optional<Saving> lastSavings(@RequestHeader("Authorization") String jwt) throws SavingException {
        User user = userService.findUserProfileByJwt(jwt).getUser();
        return savingRepository.findTopByUserIdOrderByIdDesc(user.getId());
    }

}
