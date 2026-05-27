package com.innowise.userservice.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.userservice.dto.request.CreateCardRequest;
import com.innowise.userservice.dto.request.UpdateCardRequest;
import com.innowise.userservice.repository.UserRepository;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("CardController Integration Tests")
public class CardControllerIntegrationTest {

    @org.springframework.boot.test.context.TestConfiguration
    static class JacksonTestConfig {
        @org.springframework.context.annotation.Bean
        public com.fasterxml.jackson.databind.ObjectMapper testObjectMapper() {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            return mapper;
        }
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("test_db")
            .withUsername("test_name")
            .withPassword("test_password");

    @Container
    static RedisContainer redis = new RedisContainer("redis:7.2-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Autowired
    private UserRepository userRepository;

    private UUID testUserId;
    private CreateCardRequest createCardRequest;
    private UpdateCardRequest updateCardRequest;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();

        String userJson = """
                {
                    "name": "Иван",
                    "surname": "Петров",
                    "birthDate": "1990-01-01",
                    "email": "card_controller_test_%d@example.com"
                }
                """.formatted(System.currentTimeMillis());

        String response = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        testUserId = UUID.fromString(objectMapper.readTree(response).get("id").asText());

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
    }

    @Test
    @DisplayName("POST /api/v1/cards - should create card and return 201")
    void shouldCreateCard() throws Exception {
        mockMvc.perform(post("/api/v1/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCardRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.number").value("4111111111111111"))
                .andExpect(jsonPath("$.userId").value(testUserId.toString()))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/cards - should return 400 for invalid data")
    void shouldReturnBadRequestForInvalidCard() throws Exception {
        CreateCardRequest invalidRequest = CreateCardRequest.builder()
                .userId(testUserId.toString())
                .number("invalid")
                .holder("")
                .expirationDate(LocalDate.of(2000, 1, 1))  // просрочена
                .build();

        mockMvc.perform(post("/api/v1/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/cards - should return 409 when card number already exists")
    void shouldReturnConflictWhenCardNumberExists() throws Exception {
        mockMvc.perform(post("/api/v1/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCardRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCardRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /api/v1/cards/{id} - should return card by id")
    void shouldGetCardById() throws Exception {
        String response = mockMvc.perform(post("/api/v1/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCardRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID cardId = UUID.fromString(objectMapper.readTree(response).get("id").asText());

        mockMvc.perform(get("/api/v1/cards/{id}", cardId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId.toString()))
                .andExpect(jsonPath("$.number").value("4111111111111111"));
    }

    @Test
    @DisplayName("GET /api/v1/cards/{id} - should return 404 for non-existent card")
    void shouldReturnNotFoundForNonExistentCard() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/cards/{id}", nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/cards/user/{userId} - should return all cards for user")
    void shouldGetAllCardsByUserId() throws Exception {
        mockMvc.perform(post("/api/v1/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCardRequest)))
                .andExpect(status().isCreated());

        CreateCardRequest secondRequest = CreateCardRequest.builder()
                .userId(testUserId.toString())
                .number("4222222222222222")
                .holder("IVAN PETROV")
                .expirationDate(LocalDate.of(2028, 12, 31))
                .build();

        mockMvc.perform(post("/api/v1/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/cards/user/{userId}", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].number").value("4111111111111111"))
                .andExpect(jsonPath("$[1].number").value("4222222222222222"));
    }

    @Test
    @DisplayName("PUT /api/v1/cards/{id} - should update card")
    void shouldUpdateCard() throws Exception {
        String response = mockMvc.perform(post("/api/v1/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCardRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID cardId = UUID.fromString(objectMapper.readTree(response).get("id").asText());

        mockMvc.perform(put("/api/v1/cards/{id}", cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCardRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holder").value("IVAN PETROV UPDATED"))
                .andExpect(jsonPath("$.expirationDate").value("2029-12-31"));
    }

    @Test
    @DisplayName("PATCH /api/v1/cards/{id}/active - should deactivate card")
    void shouldDeactivateCard() throws Exception {
        String response = mockMvc.perform(post("/api/v1/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCardRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID cardId = UUID.fromString(objectMapper.readTree(response).get("id").asText());

        mockMvc.perform(patch("/api/v1/cards/{id}/active", cardId)
                        .param("active", "false"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/cards/{id}", cardId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @DisplayName("DELETE /api/v1/cards/{id} - should delete card")
    void shouldDeleteCard() throws Exception {
        String response = mockMvc.perform(post("/api/v1/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCardRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID cardId = UUID.fromString(objectMapper.readTree(response).get("id").asText());

        mockMvc.perform(delete("/api/v1/cards/{id}", cardId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/cards/{id}", cardId))
                .andExpect(status().isNotFound());
    }
}
