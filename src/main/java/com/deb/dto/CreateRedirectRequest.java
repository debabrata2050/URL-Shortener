package com.deb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;
import lombok.Data;

@Data
public class CreateRedirectRequest {

    @NotBlank(message = "URL cannot be blank")
    @URL(message = "Invalid URL format")
    @Size(max = 2048, message = "URL length cannot exceed 2048 characters")
    private String url;

    @Size(max = 10, message = "Custom alias cannot exceed 10 characters")
    // Alias can be blank or null if user wants a random one
    private String alias;
}