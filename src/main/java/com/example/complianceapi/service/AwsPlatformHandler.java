package com.example.complianceapi.service;

import com.example.complianceapi.config.PlatformInstanceConfig;
import com.example.complianceapi.rules.Rule;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AwsPlatformHandler implements PlatformHandler {

    private final Map<Region, KmsClient> kmsClients = new ConcurrentHashMap<>();
    private final Map<Region, SecretsManagerClient> secretsManagerClients = new ConcurrentHashMap<>();

    @Override
    public String getPlatformName() {
        return "aws";
    }

    @Override
    public Object getClient(String service, PlatformInstanceConfig config) {
        Region region = Region.of(config.getProperties().get("region"));
        return switch (service) {
            case "kms" -> kmsClients.computeIfAbsent(region, r -> KmsClient.builder().region(r).build());
            case "secretsmanager" -> secretsManagerClients.computeIfAbsent(region, r -> SecretsManagerClient.builder().region(r).build());
            default -> throw new IllegalArgumentException("Unsupported AWS service for client creation: " + service);
        };
    }

    @Override
    public Object buildApiRequest(Rule rule, String resourceIdentifier) {
        try {
            String requestClassName = "software.amazon.awssdk.services." + rule.getService() + ".model."
                    + Character.toUpperCase(rule.getCollection().getApiCall().charAt(0)) + rule.getCollection().getApiCall().substring(1) + "Request";
            Class<?> requestClass = Class.forName(requestClassName);
            Object builder = requestClass.getMethod("builder").invoke(null);

            if (rule.getCollection().getParameters() != null) {
                for (Map.Entry<String, String> param : rule.getCollection().getParameters().entrySet()) {
                    String paramValue = param.getValue().replace("${arn}", resourceIdentifier);
                    Method builderMethod = builder.getClass().getMethod(param.getKey(), String.class);
                    builderMethod.invoke(builder, paramValue);
                }
            }
            return builder.getClass().getMethod("build").invoke(builder);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build API request for rule: " + rule.getId(), e);
        }
    }

    @Override
    public String parseServiceFromIdentifier(String resourceIdentifier) {
        try {
            return resourceIdentifier.split(":")[2];
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid ARN format: " + resourceIdentifier);
        }
    }
}
