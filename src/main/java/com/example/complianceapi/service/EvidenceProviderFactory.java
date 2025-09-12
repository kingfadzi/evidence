package com.example.complianceapi.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class EvidenceProviderFactory {

    private final Map<String, EvidenceProvider> providerMap;

    public EvidenceProviderFactory(List<EvidenceProvider> providers) {
        this.providerMap = providers.stream()
                .collect(Collectors.toMap(EvidenceProvider::getPlatformName, Function.identity()));
    }

    public EvidenceProvider getProvider(String platformName) {
        // We only have one provider for now, but this factory is ready for more.
        EvidenceProvider provider = providerMap.get(platformName.toLowerCase());
        if (provider == null) {
            throw new IllegalArgumentException("No provider found for platform: " + platformName);
        }
        return provider;
    }
}
