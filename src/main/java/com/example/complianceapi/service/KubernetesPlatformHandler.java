package com.example.complianceapi.service;

import com.example.complianceapi.config.PlatformInstanceConfig;
import com.example.complianceapi.rules.Rule;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.NetworkingV1Api;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class KubernetesPlatformHandler implements PlatformHandler {

    private final Map<String, ApiClient> clientCache = new ConcurrentHashMap<>();

    @Override
    public String getPlatformName() {
        return "kubernetes";
    }

    @Override
    public Object getClient(String service, PlatformInstanceConfig config) {
        ApiClient apiClient = getApiClient(config);
        return switch (service) {
            case "v1" -> new CoreV1Api(apiClient);
            case "apps/v1" -> new AppsV1Api(apiClient);
            case "networking.k8s.io/v1" -> new NetworkingV1Api(apiClient);
            default -> throw new IllegalArgumentException("Unsupported Kubernetes API group: " + service);
        };
    }

    private ApiClient getApiClient(PlatformInstanceConfig config) {
        String cacheKey = config.getName();
        return clientCache.computeIfAbsent(cacheKey, key -> {
            try {
                String kubeconfigPath = config.getProperties().get("kubeconfig");
                KubeConfig kubeConfig = KubeConfig.loadKubeConfig(new FileReader(kubeconfigPath));
                kubeConfig.setContext(config.getProperties().get("context"));
                return ClientBuilder.kubeconfig(kubeConfig).build();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create Kubernetes client for instance: " + key, e);
            }
        });
    }

    @Override
    public Object buildApiRequest(Rule rule, String resourceIdentifier) {
        // For the Kubernetes client, the request is not a single object but rather the parameters to the method call.
        // This method will return a map of parameters to be used with reflection.
        Map<String, String> parsedId = parseIdentifierParts(resourceIdentifier);

        return Map.of(
            "namespace", parsedId.get("namespace"),
            "name", parsedId.get("name")
        );
    }

    @Override
    public String parseServiceFromIdentifier(String resourceIdentifier) {
        return parseIdentifierParts(resourceIdentifier).get("apiVersion");
    }

    private Map<String, String> parseIdentifierParts(String identifier) {
        try {
            String[] parts = identifier.split("/");
            if (parts.length == 4) { // e.g., apps/v1/Deployment/my-app-ns/my-app
                return Map.of(
                        "apiVersion", parts[0] + "/" + parts[1],
                        "kind", parts[2],
                        "namespace", parts[3],
                        "name", parts[4]
                );
            } else if (parts.length == 3) { // e.g., v1/Pod/my-app-ns/my-pod
                return Map.of(
                        "apiVersion", parts[0],
                        "kind", parts[1],
                        "namespace", parts[2],
                        "name", parts[3]
                );
            }
            throw new IllegalArgumentException();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Kubernetes identifier format. Expected [apiVersion]/[kind]/[namespace]/[name], but got: " + identifier);
        }
    }
}
