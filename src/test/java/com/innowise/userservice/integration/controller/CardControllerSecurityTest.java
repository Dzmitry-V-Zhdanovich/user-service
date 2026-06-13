package com.innowise.userservice.integration.controller;

import com.innowise.userservice.config.SecurityConfig;
import com.innowise.userservice.controller.CardController;
import com.innowise.userservice.service.CardService;
import com.innowise.userservice.service.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CardController.class)
@Import(SecurityConfig.class)
public class CardControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CardService cardService;

    @MockitoBean
    private JwtService jwtService;

    private final UUID testCardId = UUID.randomUUID();

    @Test
    @DisplayName("Should return 401 Unauthorized when request has no JWT token")
    void shouldReturn401WhenNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/cards/" + testCardId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 204 No Content for admin endpoint when user is ADMIN")
    void shouldAllowAdminOnAdminEndpoint() throws Exception {
        UsernamePasswordAuthenticationToken adminAuth = new UsernamePasswordAuthenticationToken(
                "admin-id",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        mockMvc.perform(patch("/api/v1/cards/" + testCardId + "/active")
                        .param("active", "true")
                        .with(authentication(adminAuth)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 403 Forbidden on admin endpoints when user is regular USER")
    void shouldReturn403WhenUserTriesToAccessAdminEndpoint() throws Exception {
        UsernamePasswordAuthenticationToken userAuth = new UsernamePasswordAuthenticationToken(
                "user-id",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        mockMvc.perform(patch("/api/v1/cards/" + testCardId + "/active")
                        .param("active", "true")
                        .with(authentication(userAuth)))
                .andExpect(status().isForbidden());
    }
}
