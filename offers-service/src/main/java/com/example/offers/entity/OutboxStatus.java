package com.example.offers.entity;

public enum OutboxStatus {
    NEW,
    PROCESSING,
    SENT,
    FAILED
}