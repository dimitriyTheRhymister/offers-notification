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
public class EventResponse {

    private UUID eventId;
    private String eventType;
    private String source;
    private Instant receivedAt;
    private Instant processedAt;
}