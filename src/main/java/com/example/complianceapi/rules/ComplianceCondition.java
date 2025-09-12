package com.example.complianceapi.rules;

import lombok.Data;

@Data
public class ComplianceCondition {
    private Object equals;
    private Object notEquals;
    private String contains;
    private Boolean isTrue;
    private Boolean isFalse;
}
