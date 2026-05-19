package com.innowise.userservice.service.impl;

import com.innowise.userservice.dto.request.CreateCardRequest;
import com.innowise.userservice.dto.request.UpdateCardRequest;
import com.innowise.userservice.dto.response.CardResponse;
import com.innowise.userservice.entity.PaymentCard;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.exception.DuplicateResourceException;
import com.innowise.userservice.exception.ResourceNotFoundException;
import com.innowise.userservice.exception.TooManyCardsException;
import com.innowise.userservice.mapper.CardMapper;
import com.innowise.userservice.repository.PaymentCardRepository;
import com.innowise.userservice.repository.UserRepository;
import com.innowise.userservice.service.CardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CardServiceImpl implements CardService {

    private static final int MAX_CARDS_PER_USER = 5;

    private final PaymentCardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardMapper cardMapper;

    @Override
    @Transactional
    public CardResponse createCard(CreateCardRequest request) {
        UUID userId = UUID.fromString(request.getUserId());
        log.debug("Создание карты для пользователя: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь", userId));

        int activeCardsCount = cardRepository.countActiveCardsByUserId(userId);
        if (activeCardsCount >= MAX_CARDS_PER_USER) {
            throw new TooManyCardsException(userId, activeCardsCount);
        }

        if (cardRepository.existsByNumber(request.getNumber())) {
            throw new DuplicateResourceException("Карта", "номер", request.getNumber());
        }

        PaymentCard card = cardMapper.toEntity(request);
        PaymentCard savedCard = cardRepository.save(card);

        log.info("Создана карта с ID: {} для пользователя: {}", savedCard.getId(), userId);
        return cardMapper.toResponse(savedCard);
    }

    @Override
    public CardResponse getCardById(UUID id) {
        log.debug("Поиск карты по ID: {}", id);

        PaymentCard card = cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Карта", id));

        return cardMapper.toResponse(card);
    }

    @Override
    public List<CardResponse> getCardsByUserId(UUID userId) {
        log.debug("Получение всех карт пользователя: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Пользователь", userId);
        }

        List<PaymentCard> cards = cardRepository.findByUserId(userId);
        return cardMapper.toResponseList(cards);
    }

    @Override
    public Page<CardResponse> getCardsByUserId(UUID userId, Pageable pageable) {
        log.debug("Получение карт пользователя {} с пагинацией", userId);

        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Пользователь", userId);
        }

        Page<PaymentCard> cardPage = cardRepository.findByUserId(userId, pageable);
        return cardPage.map(cardMapper::toResponse);
    }

    @Override
    @Transactional
    public CardResponse updateCard(UUID id, UpdateCardRequest request) {
        log.debug("Обновление карты с ID: {}", id);

        PaymentCard card = cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Карта", id));

        if (request.getNumber() != null && !request.getNumber().equals(card.getNumber())) {
            if (cardRepository.existsByNumber(request.getNumber())) {
                throw new DuplicateResourceException("Карта", "номер", request.getNumber());
            }
            card.setNumber(request.getNumber());
        }

        cardMapper.updateEntityFromRequest(request, card);
        PaymentCard updatedCard = cardRepository.save(card);

        log.info("Обновлена карта с ID: {}", id);
        return cardMapper.toResponse(updatedCard);
    }

    @Override
    @Transactional
    public void setCardActiveStatus(UUID id, Boolean active) {
        log.debug("Изменение статуса карты {} на active={}", id, active);

        if (!cardRepository.existsById(id)) {
            throw new ResourceNotFoundException("Карта", id);
        }

        cardRepository.setActiveStatus(id, active);
        log.info("Статус карты {} изменён на active={}", id, active);
    }

    @Override
    @Transactional
    public void deleteCard(UUID id) {
        log.debug("Удаление карты с ID: {}", id);

        if (!cardRepository.existsById(id)) {
            throw new ResourceNotFoundException("Карта", id);
        }

        cardRepository.deleteById(id);
        log.info("Удалена карта с ID: {}", id);
    }
}
