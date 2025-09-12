package com.example.complianceapi.dto;

import lombok.Data;
import java.util.List;

@Data
public class CollectRequest {
    private String applicationId;
    private String platformName;
    private List<String> ruleSetFields;
}
