package com.innowise.userservice.unit;

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
import com.innowise.userservice.service.UserCacheService;
import com.innowise.userservice.service.impl.CardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CardService Unit Tests")
public class CardServiceUnitTest {

    private static final int MAX_CARDS_PER_USER = 5;

    @Mock
    private PaymentCardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardMapper cardMapper;

    @Mock
    private UserCacheService userCacheService;

    @InjectMocks
    private CardServiceImpl cardService;

    private UUID testUserId;
    private UUID testCardId;
    private User testUser;
    private PaymentCard testCard;
    private CreateCardRequest createCardRequest;
    private UpdateCardRequest updateCardRequest;
    private CardResponse cardResponse;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testCardId = UUID.randomUUID();

        testUser = User.builder()
                .id(testUserId)
                .name("Иван")
                .surname("Петров")
                .birthDate(LocalDate.of(1990, 1, 1))
                .email("ivan@test.com")
                .active(true)
                .paymentCards(new ArrayList<>())
                .build();

        testCard = PaymentCard.builder()
                .id(testCardId)
                .user(testUser)
                .number("4111111111111111")
                .holder("IVAN PETROV")
                .expirationDate(LocalDate.of(2028, 12, 31))
                .active(true)
                .build();

        createCardRequest = CreateCardRequest.builder()
                .userId(testUserId.toString())
                .number("4111111111111111")
                .holder("IVAN PETROV")
                .expirationDate(LocalDate.of(2028, 12, 31))
                .build();

        updateCardRequest = UpdateCardRequest.builder()
                .holder("IVAN PETROV UPDATED")
                .expirationDate(LocalDate.of(2029, 12, 31))
                .build();

        cardResponse = CardResponse.builder()
                .id(testCardId)
                .userId(testUserId)
                .number("4111111111111111")
                .holder("IVAN PETROV")
                .expirationDate(LocalDate.of(2028, 12, 31))
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("createCard() tests")
    class CreateCardTests {

        @Test
        @DisplayName("Should create card successfully")
        void shouldCreateCardSuccessfully() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(cardRepository.countByUserId(testUserId)).thenReturn(2);
            when(cardRepository.existsByNumber(createCardRequest.getNumber())).thenReturn(false);
            when(cardMapper.toEntity(createCardRequest)).thenReturn(testCard);
            when(cardRepository.save(testCard)).thenReturn(testCard);
            when(cardMapper.toResponse(testCard)).thenReturn(cardResponse);
            doNothing().when(userCacheService).evictUser(testUserId);

            // When
            CardResponse result = cardService.createCard(createCardRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testCardId);
            assertThat(result.getNumber()).isEqualTo("4111111111111111");

            verify(userCacheService).evictUser(testUserId);
            verify(cardRepository).save(testCard);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> cardService.createCard(createCardRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Пользователь");

            verify(cardRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when card limit exceeded")
        void shouldThrowExceptionWhenCardLimitExceeded() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(cardRepository.countByUserId(testUserId)).thenReturn(MAX_CARDS_PER_USER);

            // When & Then
            assertThatThrownBy(() -> cardService.createCard(createCardRequest))
                    .isInstanceOf(TooManyCardsException.class)
                    .hasMessageContaining("5");

            verify(cardRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when card number already exists")
        void shouldThrowExceptionWhenCardNumberExists() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(cardRepository.countByUserId(testUserId)).thenReturn(2);
            when(cardRepository.existsByNumber(createCardRequest.getNumber())).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> cardService.createCard(createCardRequest))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("номер");

            verify(cardRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getCardById() tests")
    class GetCardByIdTests {

        @Test
        @DisplayName("Should return card by id")
        void shouldReturnCardById() {
            // Given
            when(cardRepository.findById(testCardId)).thenReturn(Optional.of(testCard));
            when(cardMapper.toResponse(testCard)).thenReturn(cardResponse);

            // When
            CardResponse result = cardService.getCardById(testCardId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testCardId);
            assertThat(result.getNumber()).isEqualTo("4111111111111111");
        }

        @Test
        @DisplayName("Should throw exception when card not found")
        void shouldThrowExceptionWhenCardNotFound() {
            // Given
            when(cardRepository.findById(testCardId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> cardService.getCardById(testCardId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Карта");
        }
    }

    @Nested
    @DisplayName("getCardsByUserId() tests")
    class GetCardsByUserIdTests {

        @Test
        @DisplayName("Should return list of cards for user")
        void shouldReturnCardsForUser() {
            // Given
            List<PaymentCard> cards = List.of(testCard);
            List<CardResponse> cardResponses = List.of(cardResponse);

            when(userRepository.existsById(testUserId)).thenReturn(true);
            when(cardRepository.findByUserId(testUserId)).thenReturn(cards);
            when(cardMapper.toResponseList(cards)).thenReturn(cardResponses);

            // When
            List<CardResponse> result = cardService.getCardsByUserId(testUserId);

            // Then
            assertThat(result.size()).isEqualTo(1);
            assertThat(result.getFirst().getId()).isEqualTo(testCardId);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            when(userRepository.existsById(testUserId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> cardService.getCardsByUserId(testUserId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(cardRepository, never()).findByUserId(any());
        }
    }

    @Nested
    @DisplayName("updateCard() tests")
    class UpdateCardTests {

        @Test
        @DisplayName("Should update card successfully")
        void shouldUpdateCardSuccessfully() {
            // Given
            when(cardRepository.findById(testCardId)).thenReturn(Optional.of(testCard));
            when(cardRepository.save(testCard)).thenReturn(testCard);
            when(cardMapper.toResponse(testCard)).thenReturn(cardResponse);
            doNothing().when(userCacheService).evictUser(testUserId);

            // When
            CardResponse result = cardService.updateCard(testCardId, updateCardRequest);

            // Then
            assertThat(result).isNotNull();
            verify(userCacheService).evictUser(testUserId);
            verify(cardRepository).save(testCard);
        }

        @Test
        @DisplayName("Should throw exception when card not found")
        void shouldThrowExceptionWhenCardNotFound() {
            // Given
            when(cardRepository.findById(testCardId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> cardService.updateCard(testCardId, updateCardRequest))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(cardRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when new card number already exists")
        void shouldThrowExceptionWhenNewCardNumberExists() {
            // Given
            String newNumber = "4222222222222222";
            UpdateCardRequest requestWithNewNumber = UpdateCardRequest.builder()
                    .number(newNumber)
                    .build();

            when(cardRepository.findById(testCardId)).thenReturn(Optional.of(testCard));
            when(cardRepository.existsByNumber(newNumber)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> cardService.updateCard(testCardId, requestWithNewNumber))
                    .isInstanceOf(DuplicateResourceException.class);

            verify(cardRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("setCardActiveStatus() tests")
    class SetCardActiveStatusTests {

        @Test
        @DisplayName("Should deactivate card successfully")
        void shouldDeactivateCardSuccessfully() {
            // Given
            when(cardRepository.findById(testCardId)).thenReturn(Optional.of(testCard));
            doNothing().when(cardRepository).setActiveStatus(testCardId, false);
            doNothing().when(userCacheService).evictUser(testUserId);

            // When
            cardService.setCardActiveStatus(testCardId, false);

            // Then
            verify(cardRepository).setActiveStatus(testCardId, false);
            verify(userCacheService).evictUser(testUserId);
        }

        @Test
        @DisplayName("Should throw exception when card not found")
        void shouldThrowExceptionWhenCardNotFound() {
            // Given
            when(cardRepository.findById(testCardId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> cardService.setCardActiveStatus(testCardId, false))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(cardRepository, never()).setActiveStatus(any(), any());
        }
    }

    @Nested
    @DisplayName("deleteCard() tests")
    class DeleteCardTests {

        @Test
        @DisplayName("Should delete card successfully")
        void shouldDeleteCardSuccessfully() {
            // Given
            when(cardRepository.findById(testCardId)).thenReturn(Optional.of(testCard));
            doNothing().when(cardRepository).deleteById(testCardId);
            doNothing().when(userCacheService).evictUser(testUserId);

            // When
            cardService.deleteCard(testCardId);

            // Then
            verify(cardRepository).deleteById(testCardId);
            verify(userCacheService).evictUser(testUserId);
        }

        @Test
        @DisplayName("Should throw exception when card not found for deletion")
        void shouldThrowExceptionWhenCardNotFound() {
            // Given
            when(cardRepository.findById(testCardId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> cardService.deleteCard(testCardId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(cardRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("getCardsByUserId() with pagination tests")
    class GetCardsByUserIdWithPaginationTests {

        @Test
        @DisplayName("Should return page of cards when user exists and has cards")
        void shouldReturnPageOfCardsWhenUserExistsAndHasCards() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);

            Page<PaymentCard> cardPage = new PageImpl<>(List.of(testCard), pageable, 1);
            Page<CardResponse> expectedResponsePage = new PageImpl<>(List.of(cardResponse), pageable, 1);

            when(userRepository.existsById(testUserId)).thenReturn(true);
            when(cardRepository.findByUserId(testUserId, pageable)).thenReturn(cardPage);
            when(cardMapper.toResponse(testCard)).thenReturn(cardResponse);

            // When
            Page<CardResponse> result = cardService.getCardsByUserId(testUserId, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().getId()).isEqualTo(testCardId);
            assertThat(result.getContent().getFirst().getNumber()).isEqualTo("4111111111111111");
            assertThat(result.getTotalElements()).isEqualTo(1);

            verify(userRepository).existsById(testUserId);
            verify(cardRepository).findByUserId(testUserId, pageable);
            verify(cardMapper).toResponse(testCard);
        }

        @Test
        @DisplayName("Should return empty page when user exists but has no cards")
        void shouldReturnEmptyPageWhenUserExistsButHasNoCards() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<PaymentCard> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(userRepository.existsById(testUserId)).thenReturn(true);
            when(cardRepository.findByUserId(testUserId, pageable)).thenReturn(emptyPage);

            // When
            Page<CardResponse> result = cardService.getCardsByUserId(testUserId, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
            assertThat(result.getTotalPages()).isEqualTo(0);

            verify(userRepository).existsById(testUserId);
            verify(cardRepository).findByUserId(testUserId, pageable);
            verify(cardMapper, never()).toResponse(any());
        }

        @Test
        @DisplayName("Should throw exception when user does not exist")
        void shouldThrowExceptionWhenUserDoesNotExist() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);

            when(userRepository.existsById(testUserId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> cardService.getCardsByUserId(testUserId, pageable))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Пользователь")
                    .hasMessageContaining(testUserId.toString());

            verify(userRepository).existsById(testUserId);
            verify(cardRepository, never()).findByUserId(any(), any());
            verify(cardMapper, never()).toResponse(any());
        }

        @Test
        @DisplayName("Should handle second page correctly")
        void shouldHandleSecondPageCorrectly() {
            // Given
            Pageable pageable = PageRequest.of(1, 5);

            List<PaymentCard> cards = IntStream.range(0, 5)
                    .mapToObj(i -> {
                        PaymentCard card = PaymentCard.builder()
                                .id(UUID.randomUUID())
                                .user(testUser)
                                .number("411111111111111" + i)
                                .holder("IVAN PETROV")
                                .expirationDate(LocalDate.of(2028, 12, 31))
                                .active(true)
                                .build();
                        return card;
                    })
                    .collect(Collectors.toList());

            Page<PaymentCard> cardPage = new PageImpl<>(cards, pageable, 15);

            when(userRepository.existsById(testUserId)).thenReturn(true);
            when(cardRepository.findByUserId(testUserId, pageable)).thenReturn(cardPage);
            when(cardMapper.toResponse(any(PaymentCard.class))).thenAnswer(invocation -> {
                PaymentCard card = invocation.getArgument(0);
                return CardResponse.builder()
                        .id(card.getId())
                        .userId(testUserId)
                        .number(card.getNumber())
                        .holder(card.getHolder())
                        .expirationDate(card.getExpirationDate())
                        .active(card.getActive())
                        .build();
            });

            // When
            Page<CardResponse> result = cardService.getCardsByUserId(testUserId, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getNumber()).isEqualTo(1);
            assertThat(result.getSize()).isEqualTo(5);
            assertThat(result.getTotalElements()).isEqualTo(15);
            assertThat(result.getTotalPages()).isEqualTo(3);
            assertThat(result.getContent()).hasSize(5);

            verify(userRepository).existsById(testUserId);
            verify(cardRepository).findByUserId(testUserId, pageable);
            verify(cardMapper, times(5)).toResponse(any(PaymentCard.class));
        }

        @Test
        @DisplayName("Should handle large page size correctly")
        void shouldHandleLargePageSizeCorrectly() {
            // Given
            Pageable pageable = PageRequest.of(0, 100);

            List<PaymentCard> cards = IntStream.range(0, 50)
                    .mapToObj(i -> {
                        PaymentCard card = PaymentCard.builder()
                                .id(UUID.randomUUID())
                                .user(testUser)
                                .number("411111111111111" + i)
                                .holder("IVAN PETROV")
                                .expirationDate(LocalDate.of(2028, 12, 31))
                                .active(true)
                                .build();
                        return card;
                    })
                    .collect(Collectors.toList());

            Page<PaymentCard> cardPage = new PageImpl<>(cards, pageable, 50);

            when(userRepository.existsById(testUserId)).thenReturn(true);
            when(cardRepository.findByUserId(testUserId, pageable)).thenReturn(cardPage);
            when(cardMapper.toResponse(any(PaymentCard.class))).thenReturn(cardResponse);

            // When
            Page<CardResponse> result = cardService.getCardsByUserId(testUserId, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getSize()).isEqualTo(100);
            assertThat(result.getContent()).hasSize(50);
            assertThat(result.getTotalElements()).isEqualTo(50);

            verify(cardMapper, times(50)).toResponse(any(PaymentCard.class));
        }
    }
}
