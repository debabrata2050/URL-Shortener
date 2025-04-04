package com.deb.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.NaturalId;
import org.hibernate.validator.constraints.URL;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @ToString
@NoArgsConstructor
@Table(name = "redirects")
public class Redirect {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(unique = true, nullable = false, length = 10)
    @Size(min = 1, max = 10, message = "Alias must be between 1 and 10 characters")
    private String alias;

    @NotBlank(message = "URL cannot be blank")
    @Column(nullable = false, length = 2048)
    @URL(message = "Invalid URL")
    private String url;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;


    public Redirect(String alias, String url) {
        this.alias = alias;
        this.url = url;
    }
}
