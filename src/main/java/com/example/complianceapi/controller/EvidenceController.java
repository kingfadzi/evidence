package com.example.complianceapi.controller;

import com.example.complianceapi.config.ComplianceCollectorConfig;
import com.example.complianceapi.config.PlatformInstanceConfig;
import com.example.complianceapi.dto.CollectRequest;
import com.example.complianceapi.model.Evidence;
import com.example.complianceapi.service.EvidenceProvider;
import com.example.complianceapi.service.EvidenceStorageClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
public class EvidenceController {

    private final EvidenceProvider evidenceProvider;
    private final ComplianceCollectorConfig complianceCollectorConfig;
    private final EvidenceStorageClient evidenceStorageClient;

    public EvidenceController(EvidenceProvider evidenceProvider,
                              ComplianceCollectorConfig complianceCollectorConfig,
                              EvidenceStorageClient evidenceStorageClient) {
        this.evidenceProvider = evidenceProvider;
        this.complianceCollectorConfig = complianceCollectorConfig;
        this.evidenceStorageClient = evidenceStorageClient;
    }

    @PostMapping("/collect")
    public List<Evidence> collectEvidence(@RequestBody CollectRequest request) {
        PlatformInstanceConfig instanceConfig = findInstanceConfig(request.getPlatformName())
                .orElseThrow(() -> new IllegalArgumentException("Configuration not found for instance: " + request.getPlatformName()));

        List<Evidence> collectedEvidence = evidenceProvider.collect(request, instanceConfig);
        evidenceStorageClient.storeEvidence(collectedEvidence);
        return collectedEvidence;
    }

    private Optional<PlatformInstanceConfig> findInstanceConfig(String platformInstanceName) {
        return complianceCollectorConfig.getPlatforms().getInstances().values().stream()
                .flatMap(List::stream)
                .filter(c -> c.getName().equals(platformInstanceName))
                .findFirst();
    }
}
