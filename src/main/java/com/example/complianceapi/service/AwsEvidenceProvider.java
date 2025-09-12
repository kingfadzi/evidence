package com.example.complianceapi.service;

import com.example.complianceapi.config.AwsInstanceConfig;
import com.example.complianceapi.config.PlatformInstanceConfig;
import com.example.complianceapi.dto.CollectRequest;
import com.example.complianceapi.model.Evidence;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AwsEvidenceProvider implements EvidenceProvider {

    @Override
    public List<Evidence> collect(CollectRequest request, PlatformInstanceConfig instanceConfig) {
        AwsInstanceConfig awsConfig = (AwsInstanceConfig) instanceConfig;
        // In a real implementation, you would use the AWS SDK with awsConfig.getRegion() etc.
        // For now, returning dummy data.
        return request.getRuleSetFields().stream()
                .map(rule -> new Evidence(
                        request.getApplicationId(),
                        "Security",
                        rule,
                        "AWS",
                        "Dummy AWS Data Source for " + awsConfig.getName(),
                        "Collected dummy data for " + rule + " from " + awsConfig.getRegion()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public String getPlatformName() {
        return "aws";
    }
}
