package com.deb.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "click_events", indexes = {
        @Index(name = "idx_link_clicked", columnList = "link_id, clicked_at DESC"),
        @Index(name = "idx_link_country", columnList = "link_id, country_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "shortLink")
public class ClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "link_id", nullable = false)
    private ShortLink shortLink;

    @Column(name = "clicked_at", nullable = false)
    private LocalDateTime clickedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "referrer_domain", length = 255)
    private String referrerDomain;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "device_type", length = 30)
    private String deviceType;

    @Column(name = "browser_family", length = 50)
    private String browserFamily;

    @Builder.Default
    @Column(name = "is_bot", nullable = false)
    private Boolean isBot = false;
}
