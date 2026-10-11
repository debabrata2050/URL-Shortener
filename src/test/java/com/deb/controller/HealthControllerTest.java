package com.deb.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DataSource dataSource;

    @Mock
    private RedisConnectionFactory redisConnectionFactory;

    @Mock
    private ConnectionFactory rabbitConnectionFactory;

    @Mock
    private java.sql.Connection sqlConnection;

    @Mock
    private DatabaseMetaData metaData;

    @Mock
    private RedisConnection redisConnection;

    @Mock
    private Connection rabbitConnection;

    private HealthController healthController;

    @BeforeEach
    void setUp() {
        healthController = new HealthController(dataSource, redisConnectionFactory, rabbitConnectionFactory);
        mockMvc = MockMvcBuilders.standaloneSetup(healthController).build();
    }

    @Test
    @DisplayName("When all dependencies healthy, returns 200 OK with UP status")
    void testAllHealthy() throws Exception {
        when(dataSource.getConnection()).thenReturn(sqlConnection);
        when(sqlConnection.isValid(2)).thenReturn(true);
        when(sqlConnection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("MySQL");

        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");

        when(rabbitConnectionFactory.createConnection()).thenReturn(rabbitConnection);
        when(rabbitConnection.isOpen()).thenReturn(true);

        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.database.status").value("UP"))
                .andExpect(jsonPath("$.components.redis.status").value("UP"))
                .andExpect(jsonPath("$.components.rabbitMQ.status").value("UP"));
    }

    @Test
    @DisplayName("When database is down, returns 503 Service Unavailable with DOWN status")
    void testDatabaseDown() throws Exception {
        when(dataSource.getConnection()).thenThrow(new RuntimeException("Connection refused"));

        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");

        when(rabbitConnectionFactory.createConnection()).thenReturn(rabbitConnection);
        when(rabbitConnection.isOpen()).thenReturn(true);

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.components.database.status").value("DOWN"));
    }

    @Test
    @DisplayName("When Redis fails but DB healthy, returns 200 OK with DEGRADED status")
    void testRedisDegraded() throws Exception {
        when(dataSource.getConnection()).thenReturn(sqlConnection);
        when(sqlConnection.isValid(2)).thenReturn(true);
        when(sqlConnection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("MySQL");

        when(redisConnectionFactory.getConnection()).thenThrow(new RuntimeException("Redis timeout"));

        when(rabbitConnectionFactory.createConnection()).thenReturn(rabbitConnection);
        when(rabbitConnection.isOpen()).thenReturn(true);

        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEGRADED"))
                .andExpect(jsonPath("$.components.redis.status").value("DOWN"));
    }
}
