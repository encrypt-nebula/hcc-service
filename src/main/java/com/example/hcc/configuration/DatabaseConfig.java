package com.example.hcc.configuration;

import java.util.Map;

import javax.sql.DataSource;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medryte.SecretsManagerUtils;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class DatabaseConfig {

    private final SecretsManagerUtils secretsManagerUtils;

    @Value("${db.secret.name}")
    private String secretName;

    @Bean
    DataSource dataSource() throws JsonMappingException, JsonProcessingException {
        // Fetch credentials from AWS Secrets Manager once
        String secret = secretsManagerUtils.getSecret(secretName);

        // Create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();

        Map<String, Object> secretMap = objectMapper.readValue(secret, new TypeReference<Map<String, Object>>() {});

        // Create DataSource using secrets
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(String.format("jdbc:mysql://%s:3306/%s", secretMap.get("rds_url"), secretMap.get("db_name")));
        dataSource.setUsername(secretMap.get("username").toString());
        dataSource.setPassword(secretMap.get("password").toString());

        return dataSource;
    }

}

