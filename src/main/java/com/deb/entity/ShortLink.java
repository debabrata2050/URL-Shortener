package com.deb.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "short_links")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ShortLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 1, max = 32)
    @Column(name = "short_code", unique = true, nullable = false, length = 32)
    private String shortCode;

    @NotBlank
    @Size(max = 2048)
    @Column(name = "original_url", nullable = false, length = 2048)
    private String originalUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    public ShortLink(String shortCode, String originalUrl, LocalDateTime expiresAt) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.expiresAt = expiresAt;
        this.isActive = true;
        this.version = 0L;
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}
