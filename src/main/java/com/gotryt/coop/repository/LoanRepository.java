package com.gotryt.coop.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gotryt.coop.model.Loan;
import com.gotryt.coop.model.User;


public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByUser_Id(Long userId);

    Optional<Loan> findFirstByUserAndTypeAndStatusIn(User user, String type, List<String> of);
    
}
