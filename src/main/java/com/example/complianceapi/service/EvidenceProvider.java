package com.example.complianceapi.service;

import com.example.complianceapi.config.PlatformInstanceConfig;
import com.example.complianceapi.dto.CollectRequest;
import com.example.complianceapi.model.Evidence;

import java.util.List;

public interface EvidenceProvider {
    List<Evidence> collect(CollectRequest request, PlatformInstanceConfig instanceConfig);
    String getPlatformName();
}
