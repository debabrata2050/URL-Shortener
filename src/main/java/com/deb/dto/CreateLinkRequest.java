package com.deb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLinkRequest {

    @NotBlank(message = "Destination URL cannot be blank")
    @Size(max = 2048, message = "URL cannot exceed 2048 characters")
    private String originalUrl;

    @Size(min = 2, max = 32, message = "Custom slug must be between 2 and 32 characters")
    private String customSlug;

    private Integer expiresInDays;

    private LocalDateTime expiresAt;
}
