package com.deb.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedirectResponse {
    private String alias;
    private String originalUrl;
    private String shortUrl;
}