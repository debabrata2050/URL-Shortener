package com.deb.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkResponse {
    private Long id;
    private String shortCode;
    private String originalUrl;
    private String shortUrl;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private Boolean isActive;
}
