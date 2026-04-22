package com.example.cosmossample.model;

import com.azure.spring.data.cosmos.core.mapping.Container;
import com.azure.spring.data.cosmos.core.mapping.PartitionKey;

/**
 * A simple POJO stored in the "products" container.
 *
 * KEY POINT – this class does NOT import any Jackson package directly.
 * Jackson 2 is on the classpath (required by azure-core / azure-cosmos) and
 * Jackson 3 is on the classpath (shipped with Spring Boot 4), but the POJO
 * doesn't need to know which version is driving serialization.
 *
 * Spring Data Cosmos handles the serialization through azure-cosmos which
 * internally uses Jackson 2.  The annotations below come from spring-data-cosmos
 * only.
 */
@Container(containerName = "products")
public class Product {

    private String id;

    @PartitionKey
    private String category;

    private String name;
    private double price;

    public Product() {
    }

    public Product(String id, String category, String name, double price) {
        this.id = id;
        this.category = category;
        this.name = name;
        this.price = price;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return "Product{id='%s', category='%s', name='%s', price=%.2f}"
            .formatted(id, category, name, price);
    }
}
