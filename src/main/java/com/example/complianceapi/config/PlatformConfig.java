package com.example.complianceapi.config;

import lombok.Data;
import java.util.List;

@Data
public class PlatformConfig {
    private List<AwsInstanceConfig> aws;
    private List<VmwareInstanceConfig> vmware;
}
