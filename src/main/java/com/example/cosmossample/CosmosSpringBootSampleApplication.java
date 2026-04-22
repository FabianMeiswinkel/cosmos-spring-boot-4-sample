package com.example.cosmossample;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.PartitionKey;
import com.example.cosmossample.model.Product;
import com.example.cosmossample.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.UUID;

@SpringBootApplication
public class CosmosSpringBootSampleApplication {

    private static final Logger log = LoggerFactory.getLogger(CosmosSpringBootSampleApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(CosmosSpringBootSampleApplication.class, args);
    }

    @Bean
    CommandLineRunner sampleRunner(ProductRepository repository, CosmosClient cosmosClient) {
        return args -> {
            log.info("=== Cosmos DB + Jackson 2/3 Coexistence Samples ===\n");

            runScenario1_Pojo(repository);
            runScenario2_Jackson2ObjectNode(cosmosClient);
            runScenario3_Jackson3ObjectNode(cosmosClient);

            log.info("\n=== All scenarios completed ===");
        };
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SCENARIO 1 – POJO with Spring Data Cosmos
    //
    // The Product class uses ONLY spring-data-cosmos annotations (@Container,
    // @PartitionKey).  It never imports any Jackson package.  Under the hood
    // azure-cosmos serializes with Jackson 2, but the application is oblivious
    // to the Jackson version.
    // ═══════════════════════════════════════════════════════════════════════════
    private void runScenario1_Pojo(ProductRepository repository) {
        log.info("── Scenario 1: POJO via Spring Data Cosmos (Jackson-version-agnostic) ──");

        String id = UUID.randomUUID().toString();
        Product product = new Product(id, "electronics", "Wireless Mouse", 29.99);

        Product saved = repository.save(product);
        log.info("  Created : {}", saved);

        Product read = repository.findById(id, new PartitionKey("electronics")).orElseThrow();
        log.info("  Read    : {}", read);

        repository.deleteById(id, new PartitionKey("electronics"));
        log.info("  Deleted : id={}", id);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SCENARIO 2 – Jackson 2 ObjectNode (com.fasterxml.jackson.databind.node)
    //
    // This works naturally with the Cosmos SDK because the SDK uses Jackson 2
    // internally.  The application DOES import Jackson 2 packages explicitly.
    // ═══════════════════════════════════════════════════════════════════════════
    private void runScenario2_Jackson2ObjectNode(CosmosClient cosmosClient) {
        log.info("── Scenario 2: Jackson-2 ObjectNode (com.fasterxml.jackson) ──");

        // Jackson 2 ObjectMapper & ObjectNode – explicit Jackson 2 imports
        com.fasterxml.jackson.databind.ObjectMapper mapper2 =
            new com.fasterxml.jackson.databind.ObjectMapper();

        String id = UUID.randomUUID().toString();

        com.fasterxml.jackson.databind.node.ObjectNode node = mapper2.createObjectNode();
        node.put("id", id);
        node.put("category", "books");
        node.put("title", "Effective Java");
        node.put("edition", 3);

        CosmosContainer container = cosmosClient
            .getDatabase("sampledb")
            .getContainer("products");

        // Create – the SDK handles Jackson 2 ObjectNode natively
        CosmosItemResponse<com.fasterxml.jackson.databind.node.ObjectNode> createResp =
            container.createItem(node, new PartitionKey("books"), new CosmosItemRequestOptions());
        log.info("  Created : statusCode={}, RU={}", createResp.getStatusCode(), createResp.getRequestCharge());

        // Read back as a Jackson 2 ObjectNode
        CosmosItemResponse<com.fasterxml.jackson.databind.node.ObjectNode> readResp =
            container.readItem(id, new PartitionKey("books"),
                com.fasterxml.jackson.databind.node.ObjectNode.class);
        log.info("  Read    : {}", readResp.getItem());

        // Clean up
        container.deleteItem(id, new PartitionKey("books"), new CosmosItemRequestOptions());
        log.info("  Deleted : id={}", id);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SCENARIO 3 – Jackson 3 ObjectNode (tools.jackson.databind.node)
    //
    // The Cosmos SDK doesn't know about Jackson 3 types.  From its perspective
    // a Jackson-3 ObjectNode is just another bean.
    //
    // We use a *CustomItemSerializer* (Jackson3ObjectNodeSerializer) that:
    //   serialize  → converts Jackson-3 ObjectNode → Map<String,Object>
    //   deserialize→ converts Map<String,Object>   → Jackson-3 ObjectNode
    //
    // The custom serializer is attached via CosmosItemRequestOptions so it only
    // affects these specific operations.
    // ═══════════════════════════════════════════════════════════════════════════
    private void runScenario3_Jackson3ObjectNode(CosmosClient cosmosClient) {
        log.info("── Scenario 3: Jackson-3 ObjectNode (tools.jackson) + CustomItemSerializer ──");

        // Jackson 3 ObjectMapper & ObjectNode – explicit Jackson 3 imports
        tools.jackson.databind.ObjectMapper mapper3 =
            tools.jackson.databind.json.JsonMapper.builder().build();

        String id = UUID.randomUUID().toString();

        tools.jackson.databind.node.ObjectNode node3 = mapper3.createObjectNode();
        node3.put("id", id);
        node3.put("category", "music");
        node3.put("artist", "Miles Davis");
        node3.put("album", "Kind of Blue");

        CosmosContainer container = cosmosClient
            .getDatabase("sampledb")
            .getContainer("products");

        // Create – the custom serializer (set on CosmosClientBuilder) converts the Jackson-3 ObjectNode to a Map
        CosmosItemResponse<tools.jackson.databind.node.ObjectNode> createResp =
            container.createItem(node3, new PartitionKey("music"), new CosmosItemRequestOptions());
        log.info("  Created : statusCode={}, RU={}", createResp.getStatusCode(), createResp.getRequestCharge());

        // Read back as a Jackson-3 ObjectNode
        CosmosItemResponse<tools.jackson.databind.node.ObjectNode> readResp =
            container.readItem(
                id,
                new PartitionKey("music"),
                tools.jackson.databind.node.ObjectNode.class);
        log.info("  Read    : {}", readResp.getItem());

        // Clean up
        container.deleteItem(id, new PartitionKey("music"), new CosmosItemRequestOptions());
        log.info("  Deleted : id={}", id);
    }
}
