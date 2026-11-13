package com.plansecu.repository;

import com.plansecu.domain.Notification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @EntityGraph(attributePaths = "agent")
    List<Notification> findTop200ByOrderByEnvoyeLeDesc();
}
