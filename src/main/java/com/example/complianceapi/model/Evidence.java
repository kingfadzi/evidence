package com.example.complianceapi.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Evidence {

    private String applicationId;
    private String mainCategory;
    private String ruleSetField;
    private String platform;
    private String dataSource;
    private Object collectedData;
}
