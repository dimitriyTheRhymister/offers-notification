package com.example.notification;

import com.example.notification.repository.InboxRepository;
import com.example.notification.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class EventIdempotencyTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("notification_db")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort
    int port;

    @Autowired InboxRepository inboxRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        notificationRepository.deleteAll();
        inboxRepository.deleteAll();
    }

    @Test
    void first_event_returns_202_and_creates_notifications() throws Exception {
        UUID eventId = UUID.randomUUID();
        String body = createOfferCreatedEvent(eventId);

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/internal/api/v1/events")
                .then()
                .statusCode(202);

        assertThat(inboxRepository.count()).isEqualTo(1);
        assertThat(notificationRepository.count()).isEqualTo(2);
    }

    @Test
    void duplicate_event_returns_200_and_does_not_create_duplicates() throws Exception {
        UUID eventId = UUID.randomUUID();
        String body = createOfferCreatedEvent(eventId);

        // Первая доставка
        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/internal/api/v1/events")
                .then()
                .statusCode(202);

        // Повторная доставка
        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/internal/api/v1/events")
                .then()
                .statusCode(200);

        // Проверки
        assertThat(inboxRepository.count()).isEqualTo(1);
        assertThat(notificationRepository.count()).isEqualTo(2);
    }

    @Test
    void event_without_telegram_creates_only_email_notification() throws Exception {
        UUID eventId = UUID.randomUUID();
        Map<String, Object> envelope = Map.of(
                "eventId", eventId,
                "eventType", "OfferCreated",
                "occurredAt", Instant.now().toString(),
                "source", "offers-service",
                "payload", Map.of(
                        "offerId", UUID.randomUUID().toString(),
                        "customerName", "No Telegram",
                        "customerEmail", "only@email.com",
                        "title", "Only email",
                        "description", "No telegram here"
                )
        );
        String body = objectMapper.writeValueAsString(envelope);

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/internal/api/v1/events")
                .then()
                .statusCode(202);

        assertThat(notificationRepository.count()).isEqualTo(1);
        assertThat(notificationRepository.findAll().get(0).getChannel().name()).isEqualTo("EMAIL");
    }

    private String createOfferCreatedEvent(UUID eventId) throws Exception {
        Map<String, Object> envelope = Map.of(
                "eventId", eventId,
                "eventType", "OfferCreated",
                "occurredAt", Instant.now().toString(),
                "source", "offers-service",
                "payload", Map.of(
                        "offerId", UUID.randomUUID().toString(),
                        "customerName", "Ivan Petrov",
                        "customerEmail", "ivan@example.com",
                        "customerTelegram", "@ivan",
                        "title", "Need Java developer",
                        "description", "desc"
                )
        );
        return objectMapper.writeValueAsString(envelope);
    }
}