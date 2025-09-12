package com.example.complianceapi.rules;

import lombok.Data;

@Data
public class Rule {
    private String id;
    private String name;
    private String platform;
    private String service;
    private String description;
    private Collection collection;
    private Assessment assessment;
}
