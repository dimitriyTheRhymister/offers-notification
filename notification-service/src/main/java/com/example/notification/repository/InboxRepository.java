package com.example.notification.repository;

import com.example.notification.entity.InboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InboxRepository extends JpaRepository<InboxEvent, UUID> {

    Optional<InboxEvent> findByEventId(UUID eventId);
}