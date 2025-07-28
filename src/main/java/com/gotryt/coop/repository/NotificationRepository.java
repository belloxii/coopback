package com.gotryt.coop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gotryt.coop.model.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientIdOrderByTimestampDesc(Long recipientId);

    List<Notification> findByRecipientIdAndIsReadFalse(Long recipientId);

    List<Notification> findByRecipientIdAndIsReadFalseOrderByTimestampDesc(Long recipientId);

    void deleteByRecipientId(Long recipientId);
}
