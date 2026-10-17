package com.example.notification.controller;

import com.example.notification.dto.EventEnvelope;
import com.example.notification.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/api/v1/events")
@RequiredArgsConstructor
public class InternalEventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<Void> accept(@Valid @RequestBody EventEnvelope envelope) {
        boolean isNew = eventService.accept(envelope);
        return ResponseEntity.status(isNew ? HttpStatus.ACCEPTED : HttpStatus.OK).build();
    }
}