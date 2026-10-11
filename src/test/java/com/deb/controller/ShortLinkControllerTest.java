package com.deb.controller;

import com.deb.dto.CreateLinkRequest;
import com.deb.dto.LinkResponse;
import com.deb.exception.RestExceptionHandler;
import com.deb.service.ShortLinkService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ShortLinkControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ShortLinkService shortLinkService;

    @InjectMocks
    private ShortLinkController shortLinkController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(shortLinkController)
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/links creates short link and returns 201 Created")
    void testCreateLink() throws Exception {
        CreateLinkRequest request = CreateLinkRequest.builder()
                .originalUrl("https://example.com/test")
                .customSlug("myTest")
                .build();

        LinkResponse response = LinkResponse.builder()
                .id(1L)
                .shortCode("myTest")
                .originalUrl("https://example.com/test")
                .shortUrl("http://localhost:8080/r/myTest")
                .createdAt(LocalDateTime.now())
                .isActive(true)
                .build();

        when(shortLinkService.createLink(any(CreateLinkRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value("myTest"))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/r/myTest"));
    }

    @Test
    @DisplayName("GET /api/v1/links returns list of links")
    void testListLinks() throws Exception {
        LinkResponse l1 = LinkResponse.builder()
                .id(1L)
                .shortCode("code1")
                .originalUrl("https://one.com")
                .shortUrl("http://localhost:8080/r/code1")
                .isActive(true)
                .build();

        when(shortLinkService.listLinks()).thenReturn(List.of(l1));

        mockMvc.perform(get("/api/v1/links"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].shortCode").value("code1"));
    }

    @Test
    @DisplayName("PATCH /api/v1/links/{shortCode}/deactivate deactivates link")
    void testDeactivateLink() throws Exception {
        LinkResponse response = LinkResponse.builder()
                .id(1L)
                .shortCode("code1")
                .originalUrl("https://one.com")
                .isActive(false)
                .build();

        when(shortLinkService.deactivateLink("code1")).thenReturn(response);

        mockMvc.perform(patch("/api/v1/links/code1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    @DisplayName("Legacy POST /api/shorten is backward-compatible")
    void testLegacyShorten() throws Exception {
        LinkResponse response = LinkResponse.builder()
                .id(1L)
                .shortCode("legacy")
                .originalUrl("https://legacy.com")
                .shortUrl("http://localhost:8080/r/legacy")
                .isActive(true)
                .build();

        when(shortLinkService.createLink(any())).thenReturn(response);

        String json = "{\"url\":\"https://legacy.com\",\"alias\":\"legacy\"}";

        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alias").value("legacy"))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/r/legacy"));
    }
}
