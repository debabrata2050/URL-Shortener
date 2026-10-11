package com.deb.controller;

import com.deb.analytics.model.ClickEventPayload;
import com.deb.analytics.publisher.AnalyticsEventPublisher;
import com.deb.cache.TwoTierCacheService;
import com.deb.cache.model.CachedLinkDto;
import com.deb.exception.LinkNotFoundException;
import com.deb.exception.RestExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RedirectControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TwoTierCacheService twoTierCacheService;

    @Mock
    private AnalyticsEventPublisher analyticsEventPublisher;

    @InjectMocks
    private RedirectController redirectController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(redirectController)
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /r/{shortCode} returns HTTP 302 Found, Location header, and dispatches async event")
    void testHandleRedirectFound() throws Exception {
        CachedLinkDto link = CachedLinkDto.builder()
                .id(1L)
                .shortCode("demo")
                .originalUrl("https://example.com/target")
                .isActive(true)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        when(twoTierCacheService.getResolvedLink("demo")).thenReturn(link);

        mockMvc.perform(get("/r/demo")
                        .header("User-Agent", "Mozilla/5.0")
                        .header("Referer", "https://twitter.com")
                        .header("X-Forwarded-For", "203.0.113.195"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/target"))
                .andExpect(header().string("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate"));

        verify(analyticsEventPublisher).publish(any(ClickEventPayload.class));
    }

    @Test
    @DisplayName("GET /r/{shortCode} for expired link returns HTTP 410 Gone")
    void testHandleRedirectExpiredReturns410() throws Exception {
        CachedLinkDto expiredLink = CachedLinkDto.builder()
                .id(2L)
                .shortCode("expired")
                .originalUrl("https://expired.com")
                .isActive(true)
                .expiresAt(LocalDateTime.now().minusMinutes(10)) // Already expired
                .build();

        when(twoTierCacheService.getResolvedLink("expired")).thenReturn(expiredLink);

        mockMvc.perform(get("/r/expired"))
                .andExpect(status().isGone());

        verify(analyticsEventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("GET /r/{shortCode} for nonexistent link returns HTTP 404 Not Found")
    void testHandleRedirectNotFoundReturns404() throws Exception {
        when(twoTierCacheService.getResolvedLink("missing"))
                .thenThrow(new LinkNotFoundException("Link not found: missing"));

        mockMvc.perform(get("/r/missing"))
                .andExpect(status().isNotFound());

        verify(analyticsEventPublisher, never()).publish(any());
    }
}
