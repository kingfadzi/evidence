package com.example.complianceapi.service;

import com.example.complianceapi.config.AwsInstanceConfig;
import com.example.complianceapi.config.PlatformInstanceConfig;
import com.example.complianceapi.dto.CollectRequest;
import com.example.complianceapi.model.Evidence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.GetKeyRotationStatusRequest;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.DescribeSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.DescribeSecretResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AwsEvidenceProvider implements EvidenceProvider {

    private static final Logger logger = LoggerFactory.getLogger(AwsEvidenceProvider.class);

    @Override
    public List<Evidence> collect(CollectRequest request, PlatformInstanceConfig instanceConfig) {
        AwsInstanceConfig awsConfig = (AwsInstanceConfig) instanceConfig;
        Region region = Region.of(awsConfig.getRegion());
        List<Evidence> evidenceList = new ArrayList<>();

        // Create clients with try-with-resources to ensure they are closed
        try (KmsClient kmsClient = KmsClient.builder().region(region).build();
             SecretsManagerClient secretsClient = SecretsManagerClient.builder().region(region).build()) {

            for (String arn : request.getResourceArns()) {
                String service = parseServiceFromArn(arn);

                for (String rule : request.getRuleSetFields()) {
                    try {
                        switch (service) {
                            case "kms":
                                if ("Key Rotation Max".equals(rule)) {
                                    evidenceList.add(collectKeyRotationEvidence(request, awsConfig, kmsClient, arn));
                                }
                                break;
                            case "secretsmanager":
                                if ("Secrets Management".equals(rule)) {
                                    evidenceList.add(collectSecretsManagementEvidence(request, awsConfig, secretsClient, arn));
                                }
                                break;
                            default:
                                logger.warn("Unsupported service '{}' for ARN '{}'", service, arn);
                        }
                    } catch (Exception e) {
                        logger.error("Failed to collect evidence for rule '{}' on ARN '{}': {}", rule, arn, e.getMessage());
                        evidenceList.add(createErrorEvidence(request, awsConfig, rule, "Failed for ARN " + arn + ": " + e.getMessage()));
                    }
                }
            }
        }
        return evidenceList;
    }

    private String parseServiceFromArn(String arn) {
        try {
            return arn.split(":")[2];
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid ARN format: " + arn);
        }
    }

    private Evidence collectSecretsManagementEvidence(CollectRequest request, AwsInstanceConfig config, SecretsManagerClient client, String secretArn) {
        DescribeSecretRequest describeSecretRequest = DescribeSecretRequest.builder().secretId(secretArn).build();
        DescribeSecretResponse secretResponse = client.describeSecret(describeSecretRequest);

        Map<String, Object> secretData = new HashMap<>();
        secretData.put("name", secretResponse.name());
        secretData.put("arn", secretResponse.arn());
        secretData.put("lastChangedDate", secretResponse.lastChangedDate().toString());
        secretData.put("rotationEnabled", secretResponse.rotationEnabled());

        return createSuccessEvidence(request, config, "Secrets Management", "AWS Secrets Manager", secretData);
    }

    private Evidence collectKeyRotationEvidence(CollectRequest request, AwsInstanceConfig config, KmsClient client, String keyArn) {
        GetKeyRotationStatusRequest rotationStatusRequest = GetKeyRotationStatusRequest.builder().keyId(keyArn).build();
        boolean rotationEnabled = client.getKeyRotationStatus(rotationStatusRequest).keyRotationEnabled();

        Map<String, Object> keyRotationData = new HashMap<>();
        keyRotationData.put("keyArn", keyArn);
        keyRotationData.put("rotationEnabled", rotationEnabled);

        return createSuccessEvidence(request, config, "Key Rotation Max", "AWS KMS", keyRotationData);
    }

    private Evidence createSuccessEvidence(CollectRequest request, AwsInstanceConfig config, String rule, String dataSource, Object data) {
        return new Evidence(
                request.getApplicationId(),
                "Security",
                rule,
                "AWS",
                dataSource + " in " + config.getName(),
                data
        );
    }

    private Evidence createErrorEvidence(CollectRequest request, AwsInstanceConfig config, String rule, String errorMessage) {
        return new Evidence(
                request.getApplicationId(),
                "Security",
                rule,
                "AWS",
                "Error collecting from " + config.getName(),
                Map.of("error", errorMessage)
        );
    }

    @Override
    public String getPlatformName() {
        return "aws";
    }
}
