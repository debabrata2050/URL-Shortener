package com.deb.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsSummaryResponse {
    private String shortCode;
    private String originalUrl;
    private long totalClicks;
    private long uniqueIps;
    private long botClicks;
    private long humanClicks;
    private double botRatioPercentage;
}
