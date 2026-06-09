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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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

    interface TestSecurityContext extends AutoCloseable {
        @Override
        void close();
    }

    TestSecurityContext mockSecurityContext(UUID userId, String role) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userId.toString(),
                null,
                List.of(new SimpleGrantedAuthority(role))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        return SecurityContextHolder::clearContext;
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

            UserResponse expectedResponse = UserResponse.builder()
                    .id(testUserId)
                    .name(cachedUser.getName())
                    .build();
            when(userMapper.toResponse(any(UserWithCardsResponse.class)))
                    .thenReturn(expectedResponse);

            try (var ignored = mockSecurityContext(testUserId, "ROLE_USER")) {
                // When
                UserResponse result = userService.getUserById(testUserId);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getId()).isEqualTo(testUserId);
            }
        }

        @Test
        @DisplayName("Should return user from database when not in cache")
        void shouldReturnUserFromDatabaseWhenNotInCache() {
            // Given
            when(userCacheService.getCachedUser(testUserId)).thenReturn(Optional.empty());
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(cardRepository.findByUserId(testUserId)).thenReturn(new java.util.ArrayList<>());
            when(cardRepository.countByUserId(testUserId)).thenReturn(0);
            when(userMapper.toResponse(testUser)).thenReturn(userResponse);

            try (TestSecurityContext ignored = mockSecurityContext(testUserId, "ROLE_USER")) {
                // When
                UserResponse result = userService.getUserById(testUserId);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getId()).isEqualTo(testUserId);
            }

            verify(userCacheService).getCachedUser(testUserId);
            verify(userRepository).findById(testUserId);
            verify(userCacheService).cacheUser(any(UserWithCardsResponse.class));
            verify(cardRepository).countByUserId(testUserId);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            testUserId.toString(),
                            null,
                            java.util.List.of(new SimpleGrantedAuthority("ROLE_USER"))
                    );
            SecurityContextHolder.getContext().setAuthentication(auth);

            try {
                // Given
                when(userCacheService.getCachedUser(testUserId)).thenReturn(Optional.empty());
                when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

                // When & Then
                assertThatThrownBy(() -> userService.getUserById(testUserId))
                        .isInstanceOf(ResourceNotFoundException.class)
                        .hasMessageContaining("Пользователь");

            } finally {
                SecurityContextHolder.clearContext();
            }
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

            try (TestSecurityContext ignored = mockSecurityContext(testUserId, "ROLE_USER")) {
                // When
                UserResponse result = userService.updateUser(testUserId, updateRequest);

                // Then
                assertThat(result).isNotNull();
            }

            verify(userCacheService).evictUser(testUserId);
            verify(userRepository).save(testUser);
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

            try (TestSecurityContext ignored = mockSecurityContext(testUserId, "ROLE_USER")) {
                // When
                userService.deleteUser(testUserId);
            }

            verify(userRepository).deleteById(testUserId);
            verify(userCacheService).evictUser(testUserId);
        }

        @Test
        @DisplayName("Should throw exception when user not found for deletion")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            when(userRepository.existsById(testUserId)).thenReturn(false);

            try (TestSecurityContext ignored = mockSecurityContext(testUserId, "ROLE_USER")) {
                // When & Then
                assertThatThrownBy(() -> userService.deleteUser(testUserId))
                        .isInstanceOf(ResourceNotFoundException.class);
            }

            verify(userRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("setUserActiveStatus() tests")
    class SetUserActiveStatusTest {

        @Test
        @DisplayName("Should set user active status to true successfully")
        void shouldSetUserActiveStatusToTrueSuccessfully() {
            // Given
            Boolean activeStatus = true;
            when(userRepository.existsById(testUserId)).thenReturn(true);
            doNothing().when(userRepository).setActiveStatus(testUserId, activeStatus);
            doNothing().when(userCacheService).evictUser(testUserId);

            // When
            userService.setUserActiveStatus(testUserId, activeStatus);

            // Then
            verify(userRepository).existsById(testUserId);
            verify(userRepository).setActiveStatus(testUserId, activeStatus);
            verify(userCacheService).evictUser(testUserId);
            verifyNoMoreInteractions(userRepository, userCacheService);
        }

        @Test
        @DisplayName("Should set user active status to false successfully")
        void shouldSetUserActiveStatusToFalseSuccessfully() {
            // Given
            Boolean activeStatus = false;
            when(userRepository.existsById(testUserId)).thenReturn(true);
            doNothing().when(userRepository).setActiveStatus(testUserId, activeStatus);
            doNothing().when(userCacheService).evictUser(testUserId);

            // When
            userService.setUserActiveStatus(testUserId, activeStatus);

            // Then
            verify(userRepository).existsById(testUserId);
            verify(userRepository).setActiveStatus(testUserId, activeStatus);
            verify(userCacheService).evictUser(testUserId);
            verifyNoMoreInteractions(userRepository, userCacheService);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            Boolean activeStatus = true;
            when(userRepository.existsById(testUserId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> userService.setUserActiveStatus(testUserId, activeStatus))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Пользователь")
                    .hasMessageContaining(testUserId.toString());

            verify(userRepository).existsById(testUserId);
            verify(userRepository, never()).setActiveStatus(any(), any());
            verify(userCacheService, never()).evictUser(any());
        }
    }

    @Nested
    @DisplayName("getAllUsers() tests")
    class GetAllUsersTests {

        @Test
        @DisplayName("Should return page of users with card counts when users exist")
        void shouldReturnPageOfUsersWithCardCountsWhenUsersExist() {
            // Given
            String name = "Иван";
            String surname = "Петров";
            Boolean active = true;
            Pageable pageable = PageRequest.of(0, 10);

            Page<User> userPage = new PageImpl<>(List.of(testUser), pageable, 1);

            when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(userPage);

            List<UUID> userIds = List.of(testUserId);
            List<Object[]> cardCounts = List.<Object[]>of(
                    new Object[]{testUserId, 5L}
            );
            when(cardRepository.countCardsGroupByUserIds(userIds)).thenReturn(cardCounts);
            when(userMapper.toResponse(testUser)).thenReturn(userResponse);

            // When
            Page<UserResponse> result = userService.getAllUsers(name, surname, active, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().getCardsCount()).isEqualTo(5);
            assertThat(result.getContent().getFirst().getId()).isEqualTo(testUserId);

            verify(userRepository).findAll(any(Specification.class), eq(pageable));
            verify(cardRepository).countCardsGroupByUserIds(userIds);
            verify(userMapper).toResponse(testUser);
        }

        @Test
        @DisplayName("Should return page with zero card count when user has no cards")
        void shouldReturnPageWithZeroCardCountWhenUserHasNoCards() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);

            Page<User> userPage = new PageImpl<>(List.of(testUser), pageable, 1);

            when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(userPage);

            List<UUID> userIds = List.of(testUserId);
            List<Object[]> cardCounts = List.of();
            when(cardRepository.countCardsGroupByUserIds(userIds)).thenReturn(cardCounts);
            when(userMapper.toResponse(testUser)).thenReturn(userResponse);

            // When
            Page<UserResponse> result = userService.getAllUsers(null, null, null, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().getCardsCount()).isEqualTo(0);

            verify(cardRepository).countCardsGroupByUserIds(userIds);
        }

        @Test
        @DisplayName("Should return empty page when no users found")
        void shouldReturnEmptyPageWhenNoUsersFound() {
            // Given
            String name = "NonExistent";
            String surname = "User";
            Boolean active = true;
            Pageable pageable = PageRequest.of(0, 10);

            Page<User> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(emptyPage);

            // When
            Page<UserResponse> result = userService.getAllUsers(name, surname, active, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);

            verify(userRepository).findAll(any(Specification.class), eq(pageable));
            verify(cardRepository, never()).countCardsGroupByUserIds(any());
            verify(userMapper, never()).toResponse(any(User.class));
        }

        @Test
        @DisplayName("Should handle null filters correctly")
        void shouldHandleNullFiltersCorrectly() {
            // Given
            String name = null;
            String surname = null;
            Boolean active = null;
            Pageable pageable = PageRequest.of(0, 10);

            Page<User> userPage = new PageImpl<>(List.of(testUser), pageable, 1);

            when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(userPage);

            List<UUID> userIds = List.of(testUserId);
            List<Object[]> cardCounts = List.<Object[]>of(new Object[]{testUserId, 3L});
            when(cardRepository.countCardsGroupByUserIds(userIds)).thenReturn(cardCounts);
            when(userMapper.toResponse(testUser)).thenReturn(userResponse);

            // When
            Page<UserResponse> result = userService.getAllUsers(name, surname, active, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);

            verify(userRepository).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        @DisplayName("Should handle large number of users efficiently")
        void shouldHandleLargeNumberOfUsersEfficiently() {
            // Given
            String name = null;
            String surname = null;
            Boolean active = null;
            Pageable pageable = PageRequest.of(0, 1000);

            List<User> users = IntStream.range(0, 500)
                    .mapToObj(i -> User.builder()
                            .id(UUID.randomUUID())
                            .name("User" + i)
                            .build())
                    .collect(Collectors.toList());

            Page<User> userPage = new PageImpl<>(users, pageable, 500);

            when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(userPage);

            List<UUID> userIds = users.stream().map(User::getId).collect(Collectors.toList());
            List<Object[]> cardCounts = userIds.stream()
                    .limit(100)
                    .map(id -> new Object[]{id, 1L})
                    .collect(Collectors.toList());
            when(cardRepository.countCardsGroupByUserIds(userIds)).thenReturn(cardCounts);
            when(userMapper.toResponse(any(User.class))).thenReturn(userResponse);

            // When
            Page<UserResponse> result = userService.getAllUsers(name, surname, active, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(500);

            verify(cardRepository).countCardsGroupByUserIds(userIds);
            verify(userMapper, times(500)).toResponse(any(User.class));
        }
    }
}
