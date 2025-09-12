package com.example.complianceapi.rules;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Assessment {
    @JsonProperty("compliantWhen")
    private ComplianceCondition compliantWhen;
}
