package com.example.complianceapi.config;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class PlatformConfig {
    private Map<String, List<PlatformInstanceConfig>> instances;
}
