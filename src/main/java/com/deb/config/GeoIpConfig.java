package com.deb.config;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.GeoIp2Provider;
import com.maxmind.geoip2.WebServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

@Configuration
public class GeoIpConfig {

    private static final Logger log = LoggerFactory.getLogger(GeoIpConfig.class);

    @Value("${maxmind.geoip.account-id:0}")
    private int accountId;

    @Value("${maxmind.geoip.license-key:}")
    private String licenseKey;

    @Value("${maxmind.geoip.host:geolite.info}")
    private String host;

    @Value("${maxmind.geoip.database-path:}")
    private String databasePath;

    @Bean
    public GeoIp2Provider geoIp2Provider() {
        // 1. Local MMDB file if configured and present
        if (StringUtils.hasText(databasePath)) {
            File dbFile = new File(databasePath);
            if (dbFile.exists() && dbFile.isFile()) {
                try {
                    log.info("Initialized MaxMind GeoIP2 DatabaseReader from '{}'", dbFile.getAbsolutePath());
                    return new DatabaseReader.Builder(dbFile).build();
                } catch (IOException e) {
                    log.warn("Failed to initialize MaxMind DatabaseReader from '{}': {}", databasePath, e.getMessage());
                }
            } else {
                log.warn("Configured MaxMind database file not found at '{}'. Falling back.", databasePath);
            }
        }

        // 2. MaxMind GeoLite2 Web Service Client
        if (accountId > 0 && StringUtils.hasText(licenseKey)) {
            try {
                log.info("Initialized MaxMind GeoIP2 WebServiceClient (Account ID: {}, host: {})", accountId, host);
                return new WebServiceClient.Builder(accountId, licenseKey)
                        .host(host)
                        .connectTimeout(Duration.ofSeconds(3))
                        .requestTimeout(Duration.ofSeconds(3))
                        .build();
            } catch (Exception e) {
                log.warn("Failed to initialize MaxMind WebServiceClient: {}", e.getMessage());
            }
        }

        log.info("MaxMind GeoIP2 provider not configured. Using offline heuristic fallback.");
        return null;
    }
}
