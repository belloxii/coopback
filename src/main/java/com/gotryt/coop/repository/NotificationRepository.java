package com.gotryt.coop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gotryt.coop.model.Notification;


public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long id);

    void deleteByUserId(Long id);

    List<Notification> findByUserIdAndReadFalse(Long id);

    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long id);

}
