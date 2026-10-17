package com.example.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private UUID id;
    private UUID eventId;
    private String channel;
    private String recipient;
    private String subject;
    private String message;
    private String status;
    private Instant createdAt;
}