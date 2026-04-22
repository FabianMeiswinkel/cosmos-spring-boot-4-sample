package com.example.cosmossample.serializer;

import com.azure.cosmos.CosmosItemSerializer;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A custom {@link CosmosItemSerializer} that knows how to serialize / deserialize
 * Jackson-3 {@link tools.jackson.databind.node.ObjectNode} instances.
 *
 * <h2>Why is this needed?</h2>
 * The Azure Cosmos SDK uses Jackson 2 internally.  Its default serializer works
 * with POJOs and Jackson-2 tree types ({@code com.fasterxml.jackson.databind.node.ObjectNode})
 * out of the box.  Jackson 3 tree types live in a different package
 * ({@code tools.jackson.databind.node.ObjectNode}) and are just "some unknown bean"
 * from the SDK's perspective.
 *
 * <h2>How it works</h2>
 * <ul>
 *   <li>{@link #serialize} – converts a Jackson-3 ObjectNode into a
 *       {@code Map<String, Object>} (the interchange format expected by the SDK)
 *       using Jackson 3's own {@code ObjectMapper.convertValue}.</li>
 *   <li>{@link #deserialize} – takes the {@code Map<String, Object>} that the SDK
 *       produced (after JSON parsing with Jackson 2) and converts it into a
 *       Jackson-3 ObjectNode using Jackson 3's {@code ObjectMapper.convertValue}.</li>
 * </ul>
 *
 * For any type that is <em>not</em> a Jackson-3 ObjectNode the call is delegated to
 * {@link CosmosItemSerializer#DEFAULT_SERIALIZER}.
 */
public class Jackson3ObjectNodeSerializer extends CosmosItemSerializer {

    // Jackson 3 ObjectMapper – used solely for Map ↔ ObjectNode conversion
    private static final tools.jackson.databind.ObjectMapper JACKSON3_MAPPER =
        tools.jackson.databind.json.JsonMapper.builder().build();

    @Override
    @SuppressWarnings("unchecked")
    public <T> Map<String, Object> serialize(T item) {
        if (item instanceof tools.jackson.databind.node.ObjectNode objectNode) {
            // Convert the Jackson-3 tree into a plain Map that the Cosmos SDK understands.
            return toMap(objectNode);
        }

        // Not a Jackson-3 ObjectNode – delegate to the SDK's built-in serializer.
        return DEFAULT_SERIALIZER.serialize(item);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(Map<String, Object> jsonNodeMap, Class<T> classType) {
        if (classType == tools.jackson.databind.node.ObjectNode.class) {
            // Build a Jackson-3 ObjectNode from the Map the SDK hands us.
            tools.jackson.databind.node.ObjectNode node =
                JACKSON3_MAPPER.convertValue(jsonNodeMap, tools.jackson.databind.node.ObjectNode.class);
            return (T) node;
        }

        // Fall back to default for other types.
        return DEFAULT_SERIALIZER.deserialize(jsonNodeMap, classType);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /**
     * Recursively convert a Jackson 3 ObjectNode into a {@code Map<String, Object>}.
     * We avoid {@code convertValue} here to keep the map values as plain Java types
     * (String, Number, Boolean, List, Map, null) which is what the Cosmos SDK expects.
     */
    private static Map<String, Object> toMap(tools.jackson.databind.node.ObjectNode node) {
        Map<String, Object> map = new LinkedHashMap<>();
        var fields = node.properties();
        for (var entry : fields) {
            map.put(entry.getKey(), toJavaValue(entry.getValue()));
        }
        return map;
    }

    private static Object toJavaValue(tools.jackson.databind.JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.textValue();
        }
        if (node.isInt()) {
            return node.intValue();
        }
        if (node.isLong()) {
            return node.longValue();
        }
        if (node.isDouble() || node.isFloat()) {
            return node.doubleValue();
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        if (node.isObject()) {
            return toMap((tools.jackson.databind.node.ObjectNode) node);
        }
        if (node.isArray()) {
            var list = new java.util.ArrayList<>();
            for (var element : node) {
                list.add(toJavaValue(element));
            }
            return list;
        }
        // Fallback – return the text representation
        return node.toString();
    }
}
