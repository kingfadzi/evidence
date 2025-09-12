package com.example.complianceapi.controller;

import com.example.complianceapi.config.ComplianceCollectorConfig;
import com.example.complianceapi.config.PlatformInstanceConfig;
import com.example.complianceapi.dto.CollectRequest;
import com.example.complianceapi.model.Evidence;
import com.example.complianceapi.service.EvidenceProvider;
import com.example.complianceapi.service.EvidenceProviderFactory;
import com.example.complianceapi.service.EvidenceStorageClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
public class EvidenceController {

    private final EvidenceProviderFactory providerFactory;
    private final ComplianceCollectorConfig complianceCollectorConfig;
    private final EvidenceStorageClient evidenceStorageClient;

    public EvidenceController(EvidenceProviderFactory providerFactory,
                              ComplianceCollectorConfig complianceCollectorConfig,
                              EvidenceStorageClient evidenceStorageClient) {
        this.providerFactory = providerFactory;
        this.complianceCollectorConfig = complianceCollectorConfig;
        this.evidenceStorageClient = evidenceStorageClient;
    }

    @PostMapping("/collect")
    public List<Evidence> collectEvidence(@RequestBody CollectRequest request) {
        String platformName = getPlatformType(request.getPlatformName());
        EvidenceProvider provider = providerFactory.getProvider(platformName);

        PlatformInstanceConfig instanceConfig = findInstanceConfig(request.getPlatformName())
                .orElseThrow(() -> new IllegalArgumentException("Configuration not found for instance: " + request.getPlatformName()));

        List<Evidence> collectedEvidence = provider.collect(request, instanceConfig);
        evidenceStorageClient.storeEvidence(collectedEvidence);
        return collectedEvidence;
    }

    private String getPlatformType(String platformInstanceName) {
        if (complianceCollectorConfig.getPlatforms().getAws().stream().anyMatch(c -> c.getName().equals(platformInstanceName))) {
            return "aws";
        }
        if (complianceCollectorConfig.getPlatforms().getVmware().stream().anyMatch(c -> c.getName().equals(platformInstanceName))) {
            return "vmware";
        }
        throw new IllegalArgumentException("Unknown platform type for instance: " + platformInstanceName);
    }

    private Optional<PlatformInstanceConfig> findInstanceConfig(String platformInstanceName) {
        Optional<PlatformInstanceConfig> awsConfig = complianceCollectorConfig.getPlatforms().getAws().stream()
                .filter(c -> c.getName().equals(platformInstanceName))
                .map(c -> (PlatformInstanceConfig) c)
                .findFirst();

        if (awsConfig.isPresent()) {
            return awsConfig;
        }

        return complianceCollectorConfig.getPlatforms().getVmware().stream()
                .filter(c -> c.getName().equals(platformInstanceName))
                .map(c -> (PlatformInstanceConfig) c)
                .findFirst();
    }
}
