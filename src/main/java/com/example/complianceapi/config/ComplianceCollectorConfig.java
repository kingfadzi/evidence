package com.example.complianceapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "compliance-collector")
public class ComplianceCollectorConfig {
    private PlatformConfig platforms;
}
