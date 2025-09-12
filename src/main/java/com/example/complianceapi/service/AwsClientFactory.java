package com.example.complianceapi.service;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AwsClientFactory {

    private final Map<Region, KmsClient> kmsClients = new ConcurrentHashMap<>();
    private final Map<Region, SecretsManagerClient> secretsManagerClients = new ConcurrentHashMap<>();

    public Object getClient(String service, Region region) {
        return switch (service) {
            case "kms" -> kmsClients.computeIfAbsent(region, r -> KmsClient.builder().region(r).build());
            case "secretsmanager" -> secretsManagerClients.computeIfAbsent(region, r -> SecretsManagerClient.builder().region(r).build());
            default -> throw new IllegalArgumentException("Unsupported AWS service: " + service);
        };
    }
}
