package com.example.notification.service;

import com.example.notification.dto.EventEnvelope;
import com.example.notification.dto.EventResponse;
import com.example.notification.dto.NotificationResponse;
import com.example.notification.entity.InboxEvent;
import com.example.notification.entity.Notification;
import com.example.notification.entity.NotificationChannel;
import com.example.notification.entity.NotificationStatus;
import com.example.notification.repository.InboxRepository;
import com.example.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final InboxRepository inboxRepository;
    private final NotificationRepository notificationRepository;

    /**
     * Приём события. Возвращает true, если событие новое (202), false — если уже было (200).
     */
    @Transactional
    public boolean accept(EventEnvelope envelope) {
        if (inboxRepository.findByEventId(envelope.getEventId()).isPresent()) {
            log.info("Duplicate event {} ignored", envelope.getEventId());
            return false;
        }

        Instant now = Instant.now();
        InboxEvent inboxEvent = InboxEvent.builder()
                .id(UUID.randomUUID())
                .eventId(envelope.getEventId())
                .eventType(envelope.getEventType())
                .source(envelope.getSource())
                .payload(envelope.getPayload())
                .receivedAt(now)
                .processedAt(null)
                .build();
        inboxRepository.save(inboxEvent);

        if ("OfferCreated".equals(envelope.getEventType())) {
            createOfferNotifications(envelope, now);
        }

        inboxEvent.setProcessedAt(Instant.now());
        inboxRepository.save(inboxEvent);

        return true;
    }

    @Transactional(readOnly = true)
    public List<EventResponse> listEvents() {
        return inboxRepository.findAll().stream()
                .map(e -> EventResponse.builder()
                        .eventId(e.getEventId())
                        .eventType(e.getEventType())
                        .source(e.getSource())
                        .receivedAt(e.getReceivedAt())
                        .processedAt(e.getProcessedAt())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listNotifications() {
        return notificationRepository.findAll().stream()
                .map(n -> NotificationResponse.builder()
                        .id(n.getId())
                        .eventId(n.getEventId())
                        .channel(n.getChannel().name())
                        .recipient(n.getRecipient())
                        .subject(n.getSubject())
                        .message(n.getMessage())
                        .status(n.getStatus().name())
                        .createdAt(n.getCreatedAt())
                        .build())
                .toList();
    }

    private void createOfferNotifications(EventEnvelope envelope, Instant now) {
        var payload = envelope.getPayload();
        String title = asString(payload.get("title"));
        String email = asString(payload.get("customerEmail"));
        String telegram = asString(payload.get("customerTelegram"));

        if (email != null && !email.isBlank()) {
            notificationRepository.save(Notification.builder()
                    .id(UUID.randomUUID())
                    .eventId(envelope.getEventId())
                    .channel(NotificationChannel.EMAIL)
                    .recipient(email)
                    .subject("Offer created")
                    .message("Offer '" + title + "' was created")
                    .status(NotificationStatus.CREATED)
                    .createdAt(now)
                    .build());
        }

        if (telegram != null && !telegram.isBlank()) {
            notificationRepository.save(Notification.builder()
                    .id(UUID.randomUUID())
                    .eventId(envelope.getEventId())
                    .channel(NotificationChannel.TELEGRAM)
                    .recipient(telegram)
                    .subject("Offer created")
                    .message("Offer '" + title + "' was created")
                    .status(NotificationStatus.CREATED)
                    .createdAt(now)
                    .build());
        }
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }
}