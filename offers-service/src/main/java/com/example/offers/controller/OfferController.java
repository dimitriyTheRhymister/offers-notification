package com.example.offers.controller;

import com.example.offers.dto.OfferRequest;
import com.example.offers.dto.OfferResponse;
import com.example.offers.service.OfferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/offers")
@RequiredArgsConstructor
public class OfferController {

    private final OfferService offerService;

    @PostMapping
    public ResponseEntity<OfferResponse> create(@Valid @RequestBody OfferRequest request) {
        OfferResponse response = offerService.create(request);
        return ResponseEntity
                .created(URI.create("/api/v1/offers/" + response.getId()))
                .body(response);
    }

    @GetMapping("/{id}")
    public OfferResponse get(@PathVariable UUID id) {
        return offerService.get(id);
    }

    @GetMapping
    public Page<OfferResponse> list(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        return offerService.list(PageRequest.of(page, size));
    }

    @PutMapping("/{id}")
    public OfferResponse update(@PathVariable UUID id,
                                @Valid @RequestBody OfferRequest request) {
        return offerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        offerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}