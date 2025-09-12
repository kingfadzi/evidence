package com.example.complianceapi.service;

import com.example.complianceapi.config.ComplianceCollectorConfig;
import com.example.complianceapi.config.PlatformInstanceConfig;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PlatformHandlerFactory {

    private final Map<String, PlatformHandler> handlerMap;
    private final ComplianceCollectorConfig config;

    public PlatformHandlerFactory(List<PlatformHandler> handlers, ComplianceCollectorConfig config) {
        this.handlerMap = handlers.stream()
                .collect(Collectors.toMap(PlatformHandler::getPlatformName, Function.identity()));
        this.config = config;
    }

    public PlatformHandler getHandler(String platformName) {
        PlatformHandler handler = handlerMap.get(platformName.toLowerCase());
        if (handler == null) {
            // In the future, we might have handlers for vmware, openshift, etc.
            throw new IllegalArgumentException("No handler found for platform: " + platformName);
        }
        return handler;
    }

    public String getPlatformType(PlatformInstanceConfig instanceConfig) {
        return config.getPlatforms().getInstances().entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(c -> c.getName().equals(instanceConfig.getName())))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown platform type for instance: " + instanceConfig.getName()));
    }
}
