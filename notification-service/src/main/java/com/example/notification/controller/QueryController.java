package com.example.notification.controller;

import com.example.notification.dto.EventResponse;
import com.example.notification.dto.NotificationResponse;
import com.example.notification.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class QueryController {

    private final EventService eventService;

    @GetMapping("/events")
    public List<EventResponse> events() {
        return eventService.listEvents();
    }

    @GetMapping("/notifications")
    public List<NotificationResponse> notifications() {
        return eventService.listNotifications();
    }
}