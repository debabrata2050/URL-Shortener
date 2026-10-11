package com.deb.analytics.model;

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
public class ClickEventPayload implements Serializable {
    private Long linkId;
    private String shortCode;
    private LocalDateTime clickedAt;
    private String ipAddress;
    private String userAgent;
    private String referer;
}
