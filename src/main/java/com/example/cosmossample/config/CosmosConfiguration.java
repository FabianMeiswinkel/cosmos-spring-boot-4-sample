package com.example.cosmossample.config;

import com.azure.cosmos.CosmosClientBuilder;
import com.azure.spring.data.cosmos.config.AbstractCosmosConfiguration;
import com.azure.spring.data.cosmos.repository.config.EnableCosmosRepositories;
import com.example.cosmossample.serializer.Jackson3ObjectNodeSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCosmosRepositories(basePackages = "com.example.cosmossample.repository")
public class CosmosConfiguration extends AbstractCosmosConfiguration {

    @Value("${spring.cloud.azure.cosmos.endpoint}")
    private String endpoint;

    @Value("${spring.cloud.azure.cosmos.key}")
    private String key;

    @Value("${spring.cloud.azure.cosmos.database}")
    private String database;

    @Bean
    public CosmosClientBuilder cosmosClientBuilder() {
        return new CosmosClientBuilder()
            .endpoint(endpoint)
            .key(key)
            // NOTE the custom serializer can be applied here client-wide
            // or in xxxRequestOptions for individual operations (see 
            // CosmosSpringBootSampleApplication#runScenario3_Jackson3ObjectNode).
            .customItemSerializer(new Jackson3ObjectNodeSerializer());
    }

    @Override
    protected String getDatabaseName() {
        return database;
    }
}
