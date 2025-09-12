package com.example.complianceapi.config;

import lombok.Data;
import java.util.Map;

@Data
public class PlatformInstanceConfig {
    private String name;
    private Map<String, String> properties;
}
