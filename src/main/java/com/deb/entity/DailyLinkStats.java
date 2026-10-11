package com.deb.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "daily_link_stats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class DailyLinkStats {

    @EmbeddedId
    private DailyLinkStatsId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("linkId")
    @JoinColumn(name = "link_id", nullable = false)
    private ShortLink shortLink;

    @Builder.Default
    @Column(name = "total_clicks", nullable = false)
    private Long totalClicks = 0L;

    @Builder.Default
    @Column(name = "unique_ips", nullable = false)
    private Long uniqueIps = 0L;

    @Builder.Default
    @Column(name = "bot_clicks", nullable = false)
    private Long botClicks = 0L;
}
