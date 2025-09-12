package com.example.complianceapi.service;

import com.example.complianceapi.config.PlatformInstanceConfig;
import com.example.complianceapi.config.VmwareInstanceConfig;
import com.example.complianceapi.dto.CollectRequest;
import com.example.complianceapi.model.Evidence;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VmwareEvidenceProvider implements EvidenceProvider {

    @Override
    public List<Evidence> collect(CollectRequest request, PlatformInstanceConfig instanceConfig) {
        VmwareInstanceConfig vmwareConfig = (VmwareInstanceConfig) instanceConfig;
        // In a real implementation, you would use the VMware SDK with vmwareConfig.getUrl() etc.
        // For now, returning dummy data.
        return request.getRuleSetFields().stream()
                .map(rule -> new Evidence(
                        request.getApplicationId(),
                        "Integrity",
                        rule,
                        "VMware",
                        "Dummy VMware Data Source for " + vmwareConfig.getName(),
                        "Collected dummy data for " + rule + " from " + vmwareConfig.getUrl()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public String getPlatformName() {
        return "vmware";
    }
}
