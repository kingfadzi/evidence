package com.example.complianceapi.service;

import com.example.complianceapi.config.PlatformInstanceConfig;
import com.example.complianceapi.rules.Rule;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AwsPlatformHandler implements PlatformHandler {

    private final Map<Region, KmsClient> kmsClients = new ConcurrentHashMap<>();
    private final Map<Region, SecretsManagerClient> secretsManagerClients = new ConcurrentHashMap<>();
    private final Map<Region, S3Client> s3Clients = new ConcurrentHashMap<>();
    private final Map<Region, IamClient> iamClients = new ConcurrentHashMap<>();


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
            case "s3" -> s3Clients.computeIfAbsent(region, r -> S3Client.builder().region(r).build());
            // IAM is a global service, but the client still requires a region.
            case "iam" -> iamClients.computeIfAbsent(region, r -> IamClient.builder().region(r).build());
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
                    String paramValue = substituteVariables(param.getValue(), resourceIdentifier);
                    Method builderMethod = findBuilderMethod(builder.getClass(), param.getKey());
                    builderMethod.invoke(builder, paramValue);
                }
            }
            return builder.getClass().getMethod("build").invoke(builder);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build API request for rule: " + rule.getId(), e);
        }
    }

    private Method findBuilderMethod(Class<?> builderClass, String methodName) throws NoSuchMethodException {
        // This is a simplification. A real implementation would need to handle different parameter types.
        return builderClass.getMethod(methodName, String.class);
    }

    private String substituteVariables(String template, String arn) {
        if (template.contains("${arn.resource}")) {
            String resource = arn.substring(arn.lastIndexOf(":") + 1);
            if (resource.contains("/")) {
                resource = resource.substring(resource.lastIndexOf("/") + 1);
            }
            return template.replace("${arn.resource}", resource);
        }
        return template.replace("${arn}", arn);
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
