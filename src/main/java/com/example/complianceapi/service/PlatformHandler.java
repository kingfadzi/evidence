package com.example.complianceapi.service;

import com.example.complianceapi.config.PlatformInstanceConfig;
import com.example.complianceapi.rules.Rule;

public interface PlatformHandler {
    String getPlatformName();
    Object getClient(String service, PlatformInstanceConfig config);
    Object buildApiRequest(Rule rule, String resourceIdentifier);
    String parseServiceFromIdentifier(String resourceIdentifier);
}
