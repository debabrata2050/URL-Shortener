package com.deb.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferrerStatsResponse {
    private String referrerDomain;
    private long totalClicks;
    private double percentage;
}
