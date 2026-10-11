package com.deb.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;

@RestController
@RequiredArgsConstructor
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final DataSource dataSource;
    @Nullable
    private final RedisConnectionFactory redisConnectionFactory;
    @Nullable
    private final ConnectionFactory rabbitConnectionFactory;

    @GetMapping({"/health", "/api/v1/health"})
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> response = new LinkedHashMap<>();
        Map<String, Object> components = new LinkedHashMap<>();

        boolean dbOk = checkDatabase(components);
        boolean redisOk = checkRedis(components);
        boolean rabbitOk = checkRabbitMQ(components);

        // Overall status logic:
        // DB is critical (DOWN if missing).
        // Redis/RabbitMQ have graceful degradation (DEGRADED if missing).
        String overallStatus;
        HttpStatus httpStatus;

        if (!dbOk) {
            overallStatus = "DOWN";
            httpStatus = HttpStatus.SERVICE_UNAVAILABLE;
        } else if (!redisOk || !rabbitOk) {
            overallStatus = "DEGRADED";
            httpStatus = HttpStatus.OK;
        } else {
            overallStatus = "UP";
            httpStatus = HttpStatus.OK;
        }

        response.put("status", overallStatus);
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("components", components);
        response.put("system", getSystemMetrics());

        return ResponseEntity.status(httpStatus).body(response);
    }

    private boolean checkDatabase(Map<String, Object> components) {
        Map<String, Object> details = new LinkedHashMap<>();
        try (java.sql.Connection conn = dataSource.getConnection()) {
            boolean valid = conn.isValid(2);
            details.put("status", valid ? "UP" : "DOWN");
            details.put("database", conn.getMetaData().getDatabaseProductName());
            components.put("database", details);
            return valid;
        } catch (Exception e) {
            log.warn("Database health check failed: {}", e.getMessage());
            details.put("status", "DOWN");
            details.put("error", e.getMessage());
            components.put("database", details);
            return false;
        }
    }

    private boolean checkRedis(Map<String, Object> components) {
        Map<String, Object> details = new LinkedHashMap<>();
        if (redisConnectionFactory == null) {
            details.put("status", "DISABLED");
            components.put("redis", details);
            return true;
        }

        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            String ping = connection.ping();
            boolean ok = "PONG".equalsIgnoreCase(ping);
            details.put("status", ok ? "UP" : "DOWN");
            details.put("ping", ping);
            components.put("redis", details);
            return ok;
        } catch (Exception e) {
            log.warn("Redis health check failed: {}", e.getMessage());
            details.put("status", "DOWN");
            details.put("error", e.getMessage());
            components.put("redis", details);
            return false;
        }
    }

    private boolean checkRabbitMQ(Map<String, Object> components) {
        Map<String, Object> details = new LinkedHashMap<>();
        if (rabbitConnectionFactory == null) {
            details.put("status", "DISABLED");
            components.put("rabbitMQ", details);
            return true;
        }

        try (Connection connection = rabbitConnectionFactory.createConnection()) {
            boolean open = connection.isOpen();
            details.put("status", open ? "UP" : "DOWN");
            components.put("rabbitMQ", details);
            return open;
        } catch (Exception e) {
            log.warn("RabbitMQ health check failed: {}", e.getMessage());
            details.put("status", "DOWN");
            details.put("error", e.getMessage());
            components.put("rabbitMQ", details);
            return false;
        }
    }

    private Map<String, Object> getSystemMetrics() {
        Map<String, Object> sys = new LinkedHashMap<>();
        Runtime runtime = Runtime.getRuntime();
        long mb = 1024 * 1024;

        sys.put("jvmUptimeSeconds", ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
        sys.put("totalMemoryMb", runtime.totalMemory() / mb);
        sys.put("freeMemoryMb", runtime.freeMemory() / mb);
        sys.put("maxMemoryMb", runtime.maxMemory() / mb);
        sys.put("availableProcessors", runtime.availableProcessors());
        return sys;
    }
}
