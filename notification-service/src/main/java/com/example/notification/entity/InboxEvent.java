package com.example.notification.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "inbox")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InboxEvent {

    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String source;

    @Type(JsonType.class)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;
}