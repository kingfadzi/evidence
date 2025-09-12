package com.example.complianceapi.service;

import com.example.complianceapi.config.AwsInstanceConfig;
import com.example.complianceapi.config.PlatformInstanceConfig;
import com.example.complianceapi.dto.CollectRequest;
import com.example.complianceapi.model.Evidence;
import com.example.complianceapi.rules.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RuleBasedEvidenceProvider implements EvidenceProvider {

    private static final Logger logger = LoggerFactory.getLogger(RuleBasedEvidenceProvider.class);

    private final Map<String, Rule> ruleMap;
    private final AwsClientFactory awsClientFactory;

    public RuleBasedEvidenceProvider(Map<String, Rule> ruleMap, AwsClientFactory awsClientFactory) {
        this.ruleMap = ruleMap;
        this.awsClientFactory = awsClientFactory;
    }

    @Override
    public List<Evidence> collect(CollectRequest request, PlatformInstanceConfig instanceConfig) {
        AwsInstanceConfig awsConfig = (AwsInstanceConfig) instanceConfig;
        Region region = Region.of(awsConfig.getRegion());

        return request.getResourceArns().stream()
                .flatMap(arn -> request.getRuleSetFields().stream()
                        .map(ruleName -> {
                            Rule rule = ruleMap.get(ruleName);
                            if (rule == null || !rule.getService().equals(parseServiceFromArn(arn))) {
                                return null; // Skip if rule doesn't exist or doesn't apply to this ARN's service
                            }
                            return executeRule(rule, request, awsConfig, arn, region);
                        }))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Evidence executeRule(Rule rule, CollectRequest request, AwsInstanceConfig config, String arn, Region region) {
        try {
            Object client = awsClientFactory.getClient(rule.getService(), region);
            Object apiRequest = buildApiRequest(rule, arn);
            Method method = findMethod(client.getClass(), rule.getCollection().getApiCall(), apiRequest.getClass());
            Object response = method.invoke(client, apiRequest);
            Object collectedData = extractDataFromResponse(response, rule);
            return createSuccessEvidence(request, config, rule.getName(), rule.getService(), collectedData);
        } catch (Exception e) {
            logger.error("Rule execution failed for rule '{}' on ARN '{}': {}", rule.getName(), arn, e.getMessage(), e);
            return createErrorEvidence(request, config, rule.getName(), "Failed for ARN " + arn + ": " + e.getMessage());
        }
    }

    private Object buildApiRequest(Rule rule, String arn) throws Exception {
        String requestClassName = "software.amazon.awssdk.services." + rule.getService() + ".model."
                + Character.toUpperCase(rule.getCollection().getApiCall().charAt(0)) + rule.getCollection().getApiCall().substring(1) + "Request";
        Class<?> requestClass = Class.forName(requestClassName);
        Object builder = requestClass.getMethod("builder").invoke(null);

        if (rule.getCollection().getParameters() != null) {
            for (Map.Entry<String, String> param : rule.getCollection().getParameters().entrySet()) {
                String paramValue = param.getValue().replace("${arn}", arn);
                Method builderMethod = builder.getClass().getMethod(param.getKey(), String.class);
                builderMethod.invoke(builder, paramValue);
            }
        }
        return builder.getClass().getMethod("build").invoke(builder);
    }

    private Method findMethod(Class<?> clientClass, String methodName, Class<?> requestClass) throws NoSuchMethodException {
        return clientClass.getMethod(methodName, requestClass);
    }

    private Object extractDataFromResponse(Object response, Rule rule) {
        BeanWrapper responseWrapper = PropertyAccessorFactory.forBeanPropertyAccess(response);
        if (rule.getCollection().getResponseField() != null) {
            return responseWrapper.getPropertyValue(rule.getCollection().getResponseField());
        }
        if (rule.getCollection().getResponseFields() != null) {
            return rule.getCollection().getResponseFields().stream()
                    .collect(Collectors.toMap(field -> field, responseWrapper::getPropertyValue));
        }
        return "No response field specified";
    }

    private String parseServiceFromArn(String arn) {
        return arn.split(":")[2];
    }

    private Evidence createSuccessEvidence(CollectRequest request, AwsInstanceConfig config, String rule, String dataSource, Object data) {
        return new Evidence(request.getApplicationId(), "Security", rule, "AWS", dataSource + " in " + config.getName(), data);
    }

    private Evidence createErrorEvidence(CollectRequest request, AwsInstanceConfig config, String rule, String errorMessage) {
        return new Evidence(request.getApplicationId(), "Security", rule, "AWS", "Error collecting from " + config.getName(), Map.of("error", errorMessage));
    }

    @Override
    public String getPlatformName() {
        return "aws";
    }
}
