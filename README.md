# Cosmos DB + Spring Boot 4 — Jackson 2 / Jackson 3 Coexistence Sample

A Spring Boot 4 console application demonstrating how to work with **Azure Cosmos DB** when both **Jackson 2** (`com.fasterxml.jackson`) and **Jackson 3** (`tools.jackson`) are on the classpath.

## Why two Jackson versions?

| Component | Jackson version | Reason |
|---|---|---|
| Spring Boot 4 / Spring Framework 7 | **Jackson 3** (`tools.jackson.core`) | Default JSON stack for the framework |
| azure-core / azure-cosmos / azure-spring-data-cosmos | **Jackson 2** (`com.fasterxml.jackson.core`) | Required by the Azure SDK |

Both coexist on the classpath because they use **different Java package names**.

## Three scenarios

### 1. POJO via Spring Data Cosmos *(Jackson-version-agnostic)*

The `Product` class uses only **spring-data-cosmos annotations** (`@Container`, `@PartitionKey`) and plain Java Bean conventions. It never imports any Jackson package. azure-cosmos handles serialization internally with Jackson 2 — the application doesn't need to care.

### 2. Jackson 2 `ObjectNode` *(native to Cosmos SDK)*

The application explicitly imports `com.fasterxml.jackson.databind.node.ObjectNode` (Jackson 2). This works naturally because the Cosmos SDK serializes Jackson 2 tree types out of the box.

### 3. Jackson 3 `ObjectNode` + `CustomItemSerializer`

Jackson 3's `tools.jackson.databind.node.ObjectNode` lives in a different package. From the Cosmos SDK's perspective it is "just another bean", so a **`CosmosItemSerializer`** subclass (`Jackson3ObjectNodeSerializer`) bridges the gap:

* **`serialize`** — converts a Jackson 3 `ObjectNode` ➜ `Map<String,Object>` (the SDK's interchange format)
* **`deserialize`** — converts `Map<String,Object>` ➜ Jackson 3 `ObjectNode`

The custom serializer is attached per-request via `CosmosItemRequestOptions.setCustomItemSerializer(…)`.

## Prerequisites

* **Java 21+**
* A **Cosmos DB** account (or the [Cosmos DB Emulator](https://learn.microsoft.com/azure/cosmos-db/local-emulator))
* Create a database named `sampledb` with a container named `products` (partition key: `/category`)

## Running

```bash
# Set connection info
export COSMOS_URI=https://<your-account>.documents.azure.com:443/
export COSMOS_KEY=<your-key>

# Build & run
./mvnw spring-boot:run
```

On Windows (PowerShell):

```powershell
$env:COSMOS_URI = "https://<your-account>.documents.azure.com:443/"
$env:COSMOS_KEY = "<your-key>"

.\mvnw.cmd spring-boot:run
```

## Project structure

```
src/main/java/com/example/cosmossample/
├── CosmosSpringBootSampleApplication.java   # Entry point + CommandLineRunner with all 3 scenarios
├── config/
│   └── CosmosConfiguration.java             # Cosmos DB Spring Data config
├── model/
│   └── Product.java                         # POJO — no Jackson imports
├── repository/
│   └── ProductRepository.java               # Spring Data Cosmos repository
└── serializer/
    └── Jackson3ObjectNodeSerializer.java    # CustomItemSerializer for Jackson 3 ObjectNode
```