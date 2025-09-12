package com.example.complianceapi.config;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsInstanceConfig extends PlatformInstanceConfig {
    private String region;
}
