package com.example.offers.service;

import com.example.offers.dto.OfferRequest;
import com.example.offers.dto.OfferResponse;
import com.example.offers.entity.Offer;
import com.example.offers.repository.OfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OfferService {

    private final OfferRepository offerRepository;

    @Transactional
    public OfferResponse create(OfferRequest request) {
        Instant now = Instant.now();
        Offer offer = Offer.builder()
                .id(UUID.randomUUID())
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .customerTelegram(request.getCustomerTelegram())
                .title(request.getTitle())
                .description(request.getDescription())
                .createdAt(now)
                .updatedAt(now)
                .build();
        Offer saved = offerRepository.save(offer);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OfferResponse get(UUID id) {
        return offerRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new OfferNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<OfferResponse> list(Pageable pageable) {
        return offerRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional
    public OfferResponse update(UUID id, OfferRequest request) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new OfferNotFoundException(id));

        offer.setCustomerName(request.getCustomerName());
        offer.setCustomerEmail(request.getCustomerEmail());
        offer.setCustomerTelegram(request.getCustomerTelegram());
        offer.setTitle(request.getTitle());
        offer.setDescription(request.getDescription());
        offer.setUpdatedAt(Instant.now());

        return toResponse(offerRepository.save(offer));
    }

    @Transactional
    public void delete(UUID id) {
        if (!offerRepository.existsById(id)) {
            throw new OfferNotFoundException(id);
        }
        offerRepository.deleteById(id);
    }

    private OfferResponse toResponse(Offer offer) {
        return OfferResponse.builder()
                .id(offer.getId())
                .customerName(offer.getCustomerName())
                .customerEmail(offer.getCustomerEmail())
                .customerTelegram(offer.getCustomerTelegram())
                .title(offer.getTitle())
                .description(offer.getDescription())
                .createdAt(offer.getCreatedAt())
                .updatedAt(offer.getUpdatedAt())
                .build();
    }
}