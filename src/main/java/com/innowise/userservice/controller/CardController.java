package com.innowise.userservice.controller;

import com.innowise.userservice.dto.request.CreateCardRequest;
import com.innowise.userservice.dto.request.UpdateCardRequest;
import com.innowise.userservice.dto.response.CardResponse;
import com.innowise.userservice.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
@Slf4j
public class CardController {

    private final CardService cardService;

    @PostMapping
    public ResponseEntity<CardResponse> createCard(@Valid @RequestBody CreateCardRequest request) {
        log.debug("REST request to create Card: {}", request);

        CardResponse response = cardService.createCard(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CardResponse> getCardById(@PathVariable UUID id) {
        log.debug("REST request to get Card by id: {}", id);

        CardResponse response = cardService.getCardById(id);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CardResponse>> getCardsByUserId(@PathVariable UUID userId) {
        log.debug("REST request to get Cards by user id: {}", userId);

        List<CardResponse> response = cardService.getCardsByUserId(userId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}/paged")
    public ResponseEntity<Page<CardResponse>> getCardsByUserIdPaged(
            @PathVariable UUID userId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.debug("REST request to get Cards by user id with pagination: userId={}", userId);

        Page<CardResponse> page = cardService.getCardsByUserId(userId, pageable);

        return ResponseEntity.ok(page);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CardResponse> updateCard(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCardRequest request) {

        log.debug("REST request to update Card by id: {}", id);

        CardResponse response = cardService.updateCard(id, request);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<Void> setCardActiveStatus(
            @PathVariable UUID id,
            @RequestParam Boolean active) {

        log.debug("REST request to set Card active status: id={}, active={}", id, active);

        cardService.setCardActiveStatus(id, active);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCard(@PathVariable UUID id) {
        log.debug("REST request to delete Card by id: {}", id);

        cardService.deleteCard(id);

        return ResponseEntity.noContent().build();
    }
}
