package com.gotryt.coop.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gotryt.coop.model.Saving;
import com.gotryt.coop.model.User;
import com.gotryt.coop.repository.SavingRepository;
import com.gotryt.coop.repository.UserRepository;
import com.gotryt.coop.utils.TxnIdGen;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SavingServiceImpl implements SavingService {

    private final SavingRepository savingRepository;
    private final UserRepository userRepository;

@Override
@Transactional
public Saving saveNow(User user, BigDecimal amount) {
    if (user == null || user.getPlan() == null || amount == null) {
        throw new IllegalArgumentException("User, amount, or Monthly Plan cannot be null");
    }

    if (user.getPlan().compareTo(amount) != 0) {
        throw new IllegalArgumentException("Invalid amount: must match user's plan");
    }

    BigDecimal currentBalance = user.getSavingsBalance() != null ? user.getSavingsBalance() : BigDecimal.ZERO;
    BigDecimal updatedBalance = currentBalance.add(amount);

    user.setSavingsBalance(updatedBalance);
    userRepository.save(user);

    Saving newSaving = Saving.builder()
        .amount(amount)
        .txnId(TxnIdGen.generateTransactionId())
        .balance(updatedBalance)
        .status("submitted")
        .user(user)
        .build();

    log.info("Saving recorded: User={}, Amount={}, New Balance={}", user.getId(), amount, updatedBalance);

    return savingRepository.save(newSaving);
}
}