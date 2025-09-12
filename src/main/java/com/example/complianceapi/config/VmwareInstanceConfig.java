package com.example.complianceapi.config;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class VmwareInstanceConfig extends PlatformInstanceConfig {
    private String url;
}
