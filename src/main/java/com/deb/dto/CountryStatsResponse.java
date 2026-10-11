package com.deb.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CountryStatsResponse {
    private String countryCode;
    private long totalClicks;
    private double percentage;
}
