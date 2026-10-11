-- V1__init_schema.sql: MySQL schema for URL Shortener & Async Click Analytics

CREATE TABLE IF NOT EXISTS short_links (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    short_code VARCHAR(32) NOT NULL,
    original_url VARCHAR(2048) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    expires_at DATETIME(6) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE KEY uq_short_code (short_code),
    KEY idx_expires_active (expires_at, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS click_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    link_id BIGINT NOT NULL,
    clicked_at DATETIME(6) NOT NULL,
    ip_address VARCHAR(45) NULL,
    referrer_domain VARCHAR(255) NULL,
    country_code VARCHAR(2) NULL,
    city VARCHAR(100) NULL,
    device_type VARCHAR(30) NULL,
    browser_family VARCHAR(50) NULL,
    is_bot BOOLEAN NOT NULL DEFAULT FALSE,
    KEY idx_link_clicked (link_id, clicked_at DESC),
    KEY idx_link_country (link_id, country_code),
    CONSTRAINT fk_clicks_link FOREIGN KEY (link_id) REFERENCES short_links(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS daily_link_stats (
    link_id BIGINT NOT NULL,
    stat_date DATE NOT NULL,
    total_clicks BIGINT NOT NULL DEFAULT 0,
    unique_ips BIGINT NOT NULL DEFAULT 0,
    bot_clicks BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (link_id, stat_date),
    CONSTRAINT fk_daily_link FOREIGN KEY (link_id) REFERENCES short_links(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
