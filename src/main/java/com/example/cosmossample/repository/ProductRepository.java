package com.example.cosmossample.repository;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import com.example.cosmossample.model.Product;
import org.springframework.stereotype.Repository;

/**
 * Spring Data Cosmos repository for the {@link Product} POJO.
 *
 * This is the "Scenario 1" entry-point: the app works with plain POJOs and
 * never has to care whether Jackson 2 or Jackson 3 drives the wire format.
 */
@Repository
public interface ProductRepository extends CosmosRepository<Product, String> {
}
