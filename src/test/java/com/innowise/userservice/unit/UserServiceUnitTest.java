package com.innowise.userservice.unit;

import com.innowise.userservice.dto.request.CreateUserRequest;
import com.innowise.userservice.dto.request.UpdateUserRequest;
import com.innowise.userservice.dto.response.UserResponse;
import com.innowise.userservice.dto.response.UserWithCardsResponse;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.exception.DuplicateResourceException;
import com.innowise.userservice.exception.ResourceNotFoundException;
import com.innowise.userservice.mapper.CardMapper;
import com.innowise.userservice.mapper.UserMapper;
import com.innowise.userservice.repository.PaymentCardRepository;
import com.innowise.userservice.repository.UserRepository;
import com.innowise.userservice.service.UserCacheService;
import com.innowise.userservice.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
public class UserServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentCardRepository cardRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CardMapper cardMapper;

    @Mock
    private UserCacheService userCacheService;

    @InjectMocks
    private UserServiceImpl userService;

    private UUID testUserId;
    private User testUser;
    private CreateUserRequest createRequest;
    private UpdateUserRequest updateRequest;
    private UserResponse userResponse;
    private UserWithCardsResponse cachedUser;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();

        testUser = User.builder()
                .id(testUserId)
                .name("Иван")
                .surname("Петров")
                .birthDate(LocalDate.of(1990, 1, 1))
                .email("ivan@test.com")
                .active(true)
                .build();

        createRequest = CreateUserRequest.builder()
                .name("Иван")
                .surname("Петров")
                .birthDate(LocalDate.of(1990, 1, 1))
                .email("ivan@test.com")
                .build();

        updateRequest = UpdateUserRequest.builder()
                .name("Иван-Обновлённый")
                .surname("Петров-Обновлённый")
                .build();

        userResponse = UserResponse.builder()
                .id(testUserId)
                .name("Иван")
                .surname("Петров")
                .birthDate(LocalDate.of(1990, 1, 1))
                .email("ivan@test.com")
                .active(true)
                .cardsCount(0)
                .build();

        cachedUser = UserWithCardsResponse.builder()
                .id(testUserId)
                .name("Иван")
                .surname("Петров")
                .active(true)
                .cards(null)
                .build();
    }

    @Nested
    @DisplayName("createUser() tests")
    class CreateUserTests {

        @Test
        @DisplayName("Should create user successfully")
        void shouldCreateUserSuccessfully() {
            // Given
            when(userRepository.existsByEmail(createRequest.getEmail())).thenReturn(false);
            when(userMapper.toEntity(createRequest)).thenReturn(testUser);
            when(userRepository.save(testUser)).thenReturn(testUser);
            when(userMapper.toResponse(testUser)).thenReturn(userResponse);

            // When
            UserResponse result = userService.createUser(createRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testUserId);
            assertThat(result.getEmail()).isEqualTo("ivan@test.com");

            verify(userRepository).existsByEmail(createRequest.getEmail());
            verify(userRepository).save(testUser);
            verify(userCacheService, never()).evictUser(any());
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailExists() {
            // Given
            when(userRepository.existsByEmail(createRequest.getEmail())).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> userService.createUser(createRequest))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("email");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getUserById() tests")
    class GetUserByIdTests {

        @Test
        @DisplayName("Should return user from cache when available")
        void shouldReturnUserFromCache() {
            // Given
            when(userCacheService.getCachedUser(testUserId)).thenReturn(Optional.of(cachedUser));

            // When
            UserResponse result = userService.getUserById(testUserId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testUserId);

            verify(userCacheService).getCachedUser(testUserId);
            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Should return user from database when not in cache")
        void shouldReturnUserFromDatabaseWhenNotInCache() {
            // Given
            when(userCacheService.getCachedUser(testUserId)).thenReturn(Optional.empty());
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(cardRepository.findByUserId(testUserId)).thenReturn(new ArrayList<>());
            when(cardRepository.countByUserId(testUserId)).thenReturn(0);
            when(userMapper.toResponse(testUser)).thenReturn(userResponse);

            // When
            UserResponse result = userService.getUserById(testUserId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testUserId);

            verify(userCacheService).getCachedUser(testUserId);
            verify(userRepository).findById(testUserId);
            verify(userCacheService).cacheUser(any(UserWithCardsResponse.class));
            verify(cardRepository).countByUserId(testUserId);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            when(userCacheService.getCachedUser(testUserId)).thenReturn(Optional.empty());
            when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.getUserById(testUserId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Пользователь");
        }
    }

    @Nested
    @DisplayName("updateUser() tests")
    class UpdateUserTests {

        @Test
        @DisplayName("Should update user successfully")
        void shouldUpdateUserSuccessfully() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(userRepository.save(testUser)).thenReturn(testUser);
            when(userMapper.toResponse(testUser)).thenReturn(userResponse);
            doNothing().when(userCacheService).evictUser(testUserId);

            // When
            UserResponse result = userService.updateUser(testUserId, updateRequest);

            // Then
            assertThat(result).isNotNull();
            verify(userCacheService).evictUser(testUserId);
        }
    }

    @Nested
    @DisplayName("deleteUser() tests")
    class DeleteUserTests {

        @Test
        @DisplayName("Should delete user successfully")
        void shouldDeleteUserSuccessfully() {
            // Given
            when(userRepository.existsById(testUserId)).thenReturn(true);
            doNothing().when(userRepository).deleteById(testUserId);
            doNothing().when(userCacheService).evictUser(testUserId);

            // When
            userService.deleteUser(testUserId);

            // Then
            verify(userRepository).deleteById(testUserId);
            verify(userCacheService).evictUser(testUserId);
        }

        @Test
        @DisplayName("Should throw exception when user not found for deletion")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            when(userRepository.existsById(testUserId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> userService.deleteUser(testUserId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(userRepository, never()).deleteById(any());
        }
    }
}
