package com.example.offers.service;

import java.util.UUID;

public class OfferNotFoundException extends RuntimeException {

    public OfferNotFoundException(UUID id) {
        super("Offer not found: " + id);
    }
}