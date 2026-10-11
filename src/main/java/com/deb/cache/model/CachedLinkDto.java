package com.deb.cache.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CachedLinkDto implements Serializable {

    private Long id;
    private String shortCode;
    private String originalUrl;
    private LocalDateTime expiresAt;
    private Boolean isActive;
    private Boolean isSentinel;

    public boolean isSentinel() {
        return Boolean.TRUE.equals(isSentinel);
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public static CachedLinkDto createSentinel(String shortCode) {
        return CachedLinkDto.builder()
                .shortCode(shortCode)
                .isSentinel(true)
                .isActive(false)
                .build();
    }
}
