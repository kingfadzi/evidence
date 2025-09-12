package com.example.complianceapi.config;

import com.example.complianceapi.rules.Rule;
import com.example.complianceapi.rules.RuleSet;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class RuleSetConfiguration {

    @Bean
    public Map<String, Rule> ruleMap() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath*:**/*-ruleset.yml");

        return Arrays.stream(resources)
                .flatMap(resource -> {
                    try {
                        return mapper.readValue(resource.getInputStream(), RuleSet.class).getRules().stream();
                    } catch (IOException e) {
                        throw new IllegalStateException("Failed to parse ruleset file: " + resource.getFilename(), e);
                    }
                })
                .collect(Collectors.toMap(Rule::getName, Function.identity()));
    }
}
