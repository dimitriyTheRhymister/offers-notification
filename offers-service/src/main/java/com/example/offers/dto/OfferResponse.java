package com.example.offers.dto;

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
public class OfferResponse {

    private UUID id;
    private String customerName;
    private String customerEmail;
    private String customerTelegram;
    private String title;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
}