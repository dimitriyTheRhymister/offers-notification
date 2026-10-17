package com.example.offers.outbox;

import com.example.offers.dto.EventEnvelope;
import com.example.offers.entity.OutboxEntry;
import com.example.offers.entity.OutboxStatus;
import com.example.offers.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxWorker {

    private final OutboxRepository outboxRepository;
    private final WebClient.Builder webClientBuilder;

    @Value("${notification.service.url}")
    private String notificationUrl;

    @Value("${outbox.worker.batch-size:20}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${outbox.worker.fixed-delay-ms:5000}")
    public void process() {
        List<OutboxEntry> entries = claimBatch();
        for (OutboxEntry entry : entries) {
            send(entry);
        }
    }

    @Transactional
    protected List<OutboxEntry> claimBatch() {
        Instant now = Instant.now();
        List<OutboxEntry> candidates = outboxRepository.findReadyForProcessing(
                List.of(OutboxStatus.NEW, OutboxStatus.FAILED),
                now,
                PageRequest.of(0, batchSize));

        for (OutboxEntry entry : candidates) {
            entry.setStatus(OutboxStatus.PROCESSING);
            entry.setUpdatedAt(now);
        }
        return outboxRepository.saveAll(candidates);
    }

    private void send(OutboxEntry entry) {
        try {
            EventEnvelope envelope = toEnvelope(entry);
            webClientBuilder.build()
                    .post()
                    .uri(notificationUrl)
                    .bodyValue(envelope)
                    .retrieve()
                    .toBodilessEntity()
                    .block(Duration.ofSeconds(10));
            markSent(entry);
        } catch (Exception ex) {
            log.warn("Failed to send event {}: {}", entry.getEventId(), ex.getMessage());
            markFailed(entry);
        }
    }

    @Transactional
    protected void markSent(OutboxEntry entry) {
        entry.setStatus(OutboxStatus.SENT);
        entry.setUpdatedAt(Instant.now());
        outboxRepository.save(entry);
    }

    @Transactional
    protected void markFailed(OutboxEntry entry) {
        Instant now = Instant.now();
        int retry = entry.getRetryCount() + 1;
        long delaySeconds = Math.min(60, (long) Math.pow(2, Math.min(retry, 6)));
        entry.setStatus(OutboxStatus.FAILED);
        entry.setRetryCount(retry);
        entry.setNextAttemptAt(now.plusSeconds(delaySeconds));
        entry.setUpdatedAt(now);
        outboxRepository.save(entry);
    }

    private EventEnvelope toEnvelope(OutboxEntry entry) {
        return EventEnvelope.builder()
                .eventId(entry.getEventId())
                .eventType(entry.getEventType())
                .occurredAt(entry.getCreatedAt())
                .source("offers-service")
                .payload(entry.getPayload())
                .build();
    }
}