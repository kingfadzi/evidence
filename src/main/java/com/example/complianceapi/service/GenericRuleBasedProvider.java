package com.example.complianceapi.service;

import com.example.complianceapi.config.PlatformInstanceConfig;
import com.example.complianceapi.dto.CollectRequest;
import com.example.complianceapi.model.Evidence;
import com.example.complianceapi.rules.Rule;
import com.jayway.jsonpath.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GenericRuleBasedProvider implements EvidenceProvider {

    private static final Logger logger = LoggerFactory.getLogger(GenericRuleBasedProvider.class);

    private final Map<String, Rule> ruleMap;
    private final PlatformHandlerFactory platformHandlerFactory;

    public GenericRuleBasedProvider(Map<String, Rule> ruleMap, PlatformHandlerFactory platformHandlerFactory) {
        this.ruleMap = ruleMap;
        this.platformHandlerFactory = platformHandlerFactory;
    }

    @Override
    public List<Evidence> collect(CollectRequest request, PlatformInstanceConfig instanceConfig) {
        String platformType = platformHandlerFactory.getPlatformType(instanceConfig);
        PlatformHandler handler = platformHandlerFactory.getHandler(platformType);

        return request.getResourceArns().stream()
                .flatMap(arn -> request.getRuleSetFields().stream()
                        .map(ruleName -> {
                            Rule rule = ruleMap.get(ruleName);
                            if (rule == null || !rule.getService().equals(handler.parseServiceFromIdentifier(arn))) {
                                return null;
                            }
                            return executeRule(rule, request, instanceConfig, arn, handler);
                        }))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Evidence executeRule(Rule rule, CollectRequest request, PlatformInstanceConfig config, String identifier, PlatformHandler handler) {
        try {
            Object client = handler.getClient(rule.getService(), config);
            Object apiRequest = handler.buildApiRequest(rule, identifier);

            Object response;
            if (apiRequest instanceof Map) {
                // Handle parameter-based API calls (like Kubernetes)
                Map<String, Object> params = (Map<String, Object>) apiRequest;
                // This is a simplification; a real implementation would need a more robust method matching logic
                Method method = findMethodByName(client.getClass(), rule.getCollection().getApiCall());
                response = method.invoke(client, params.get("name"), params.get("namespace"), null, null, null, null, null, null, null, null, null);
            } else {
                // Handle request-object-based API calls (like AWS)
                Method method = findMethod(client.getClass(), rule.getCollection().getApiCall(), apiRequest.getClass());
                response = method.invoke(client, apiRequest);
            }

            Object collectedData = extractDataFromResponse(response, rule);
            return createSuccessEvidence(request, config, rule, collectedData);
        } catch (Exception e) {
            logger.error("Rule execution failed for rule '{}' on identifier '{}': {}", rule.getName(), identifier, e.getMessage(), e);
            return createErrorEvidence(request, config, rule, "Failed for identifier " + identifier + ": " + e.getMessage());
        }
    }

    private Method findMethod(Class<?> clientClass, String methodName, Class<?> requestClass) throws NoSuchMethodException {
        return clientClass.getMethod(methodName, requestClass);
    }

    private Method findMethodByName(Class<?> clientClass, String methodName) throws NoSuchMethodException {
        // WARNING: This is a major simplification. It assumes the method signature is always the same.
        // A robust implementation would need to inspect parameter types and match them to the map.
        for (Method method : clientClass.getMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        throw new NoSuchMethodException("No method found with name: " + methodName);
    }

    private Object extractDataFromResponse(Object response, Rule rule) {
        // For Kubernetes responses, we need to use JSONPath if the response is a complex object.
        // This is a simple heuristic. A more robust solution would be handler-specific.
        if (response.getClass().getPackageName().startsWith("io.kubernetes.client")) {
            if (".".equals(rule.getCollection().getResponseField())) {
                return response.toString(); // Or serialize to JSON
            }
            // This requires the response object to be serializable to JSON.
            // The Kubernetes client objects are.
            return JsonPath.read(response.toString(), "$." + rule.getCollection().getResponseField());
        }

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

    private Evidence createSuccessEvidence(CollectRequest request, PlatformInstanceConfig config, Rule rule, Object data) {
        return new Evidence(request.getApplicationId(), "Security", rule.getName(), rule.getPlatform().toUpperCase(),
                rule.getService() + " in " + config.getName(), data);
    }

    private Evidence createErrorEvidence(CollectRequest request, PlatformInstanceConfig config, Rule rule, String errorMessage) {
        return new Evidence(request.getApplicationId(), "Security", rule.getName(), rule.getPlatform().toUpperCase(),
                "Error collecting from " + config.getName(), Map.of("error", errorMessage));
    }

    @Override
    public String getPlatformName() {
        // This provider is generic and supports all platforms defined by its handlers.
        // The factory will determine which handler to use.
        return "generic";
    }
}