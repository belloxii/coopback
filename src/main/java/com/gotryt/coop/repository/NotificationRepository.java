package com.gotryt.coop.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gotryt.coop.model.Notification;


public interface NotificationRepository extends JpaRepository<Notification, Long> {

}
