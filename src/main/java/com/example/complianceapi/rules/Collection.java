package com.example.complianceapi.rules;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class Collection {
    private String apiCall;
    private Map<String, String> parameters;
    private String responseField;
    private List<String> responseFields;
}
