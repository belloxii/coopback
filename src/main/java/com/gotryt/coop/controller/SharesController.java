package com.gotryt.coop.controller;

import com.gotryt.coop.dto.SharesResponse;
import com.gotryt.coop.exception.SharesException;
import com.gotryt.coop.model.Shares;
import com.gotryt.coop.model.User;
import com.gotryt.coop.repository.SharesRepository;
import com.gotryt.coop.service.SharesService;
import com.gotryt.coop.service.UserService;

import lombok.AllArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shares")
@AllArgsConstructor
public class SharesController {

    private final UserService userService;
    private final SharesService sharesService;
    private final SharesRepository sharesRepository;

    @PostMapping("/add")
    public ResponseEntity<Shares> addShares(@RequestHeader("Authorization") String jwt, @RequestBody Shares sharesDetails) throws SharesException {
        User user = userService.findUserProfileByJwt(jwt).getUser();
        Shares result = sharesService.addShares(user, sharesDetails);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/withdraw")
    public SharesResponse withdrawShares(@RequestHeader("Authorization") String jwt, @RequestBody Shares sharesDetails) throws SharesException {
        User user = userService.findUserProfileByJwt(jwt).getUser();
        return sharesService.withdrawShares(user, sharesDetails);
    }

    @PutMapping("/{shareId}/approve")
    public SharesResponse approveWithdraw(@RequestHeader("Authorization") String jwt, @PathVariable Long shareId) throws SharesException {
        return sharesService.approveWithdraw(shareId);
    }

    @PostMapping("/{shareId}/decline")
    public SharesResponse declineWithdraw(@RequestHeader("Authorization") String jwt, @PathVariable Long shareId, @RequestBody Shares sharesDetails) throws SharesException {
        return sharesService.declineWithdraw(shareId, sharesDetails);
    }

    @GetMapping("/my")
    public ResponseEntity<List<Shares>> getMyShares(@RequestHeader("Authorization") String jwt) throws SharesException {
        User user = userService.findUserProfileByJwt(jwt).getUser();
        return ResponseEntity.ok(sharesRepository.findByUser_Id(user.getId()));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Shares>> getUserShares(@RequestHeader("Authorization") String jwt, @PathVariable Long userId) {
        return ResponseEntity.ok(sharesRepository.findByUser_Id(userId));
    }

}
