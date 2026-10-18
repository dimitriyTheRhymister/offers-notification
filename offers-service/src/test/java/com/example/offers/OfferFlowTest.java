package com.example.offers;

import com.example.offers.repository.OfferRepository;
import com.example.offers.repository.OutboxRepository;
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

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OfferFlowTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("offers_db")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Отключаем реальную отправку — тест не должен стучаться в notification-service
        registry.add("notification.service.url", () -> "http://localhost:1/never");
    }

    @LocalServerPort
    int port;

    @Autowired
    OfferRepository offerRepository;

    @Autowired
    OutboxRepository outboxRepository;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        outboxRepository.deleteAll();
        offerRepository.deleteAll();
    }

    @Test
    void create_offer_saves_to_offers_and_outbox() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "customerName", "Ivan Petrov",
                "customerEmail", "ivan@example.com",
                "customerTelegram", "@ivan",
                "title", "Need Java developer",
                "description", "Need help with Spring Boot project"
        ));

        String id = given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/api/v1/offers")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("customerName", equalTo("Ivan Petrov"))
                .body("customerEmail", equalTo("ivan@example.com"))
                .extract().path("id");

        // Проверяем, что заявка есть в БД
        assertThat(offerRepository.findAll()).hasSize(1);

        // Проверяем, что outbox-запись создана
        assertThat(outboxRepository.findAll())
                .hasSize(1)
                .allMatch(o -> "OfferCreated".equals(o.getEventType()))
                .allMatch(o -> o.getAggregateId().toString().equals(id));
    }

    @Test
    void get_offer_by_id_returns_200() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "customerName", "Test",
                "customerEmail", "test@example.com",
                "customerTelegram", "@test",
                "title", "Test",
                "description", "Test"
        ));

        String id = given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/api/v1/offers")
                .then().statusCode(201)
                .extract().path("id");

        given()
                .when().get("/api/v1/offers/" + id)
                .then()
                .statusCode(200)
                .body("id", equalTo(id))
                .body("customerName", equalTo("Test"));
    }

    @Test
    void get_unknown_offer_returns_404() {
        given()
                .when().get("/api/v1/offers/00000000-0000-0000-0000-000000000000")
                .then()
                .statusCode(404);
    }
}