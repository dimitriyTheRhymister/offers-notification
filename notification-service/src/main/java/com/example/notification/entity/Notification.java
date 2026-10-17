package com.example.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Column(nullable = false)
    private String recipient;

    private String subject;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}