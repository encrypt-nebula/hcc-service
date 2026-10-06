package com.example.hcc.configuration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medryte.SecretsManagerUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class S3Config {

    private final SecretsManagerUtils secretsManagerUtils;

    @Value("${aws.region}")
    private String region;

    @Value("${s3.secret.name}")
    private String secretName;

    private AwsBasicCredentials getCredentials() throws Exception {
        String secret = secretsManagerUtils.getSecret(secretName);
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> secretMap =
                objectMapper.readValue(secret, new TypeReference<Map<String, String>>() {});

        String accessKey = secretMap.get("access_key");
        String secretKey = secretMap.get("secret_key");
        return AwsBasicCredentials.create(accessKey, secretKey);
    }

    @Bean
    public S3Presigner s3Presigner() throws Exception {
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(getCredentials()))
                .build();
    }

    @Bean
    public S3Client s3Client() throws Exception {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(getCredentials()))
                .build();
    }
}
