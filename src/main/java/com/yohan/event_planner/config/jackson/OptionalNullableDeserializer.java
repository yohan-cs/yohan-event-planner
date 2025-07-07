package com.yohan.event_planner.config.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.Optional;

/**
 * Custom Jackson deserializer for handling Optional fields that can be explicitly null.
 * 
 * <p>This deserializer handles three cases for PATCH operations:
 * <ul>
 *   <li>Field absent from JSON: returns null (indicating "no change")</li>
 *   <li>Field present with value: returns Optional.of(value)</li>
 *   <li>Field present with null: returns Optional.empty() (indicating "clear field")</li>
 * </ul>
 * </p>
 */
public class OptionalNullableDeserializer extends StdDeserializer<Optional<?>> implements ContextualDeserializer {

    private final JavaType valueType;

    public OptionalNullableDeserializer() {
        this(null);
    }

    public OptionalNullableDeserializer(JavaType valueType) {
        super(Optional.class);
        this.valueType = valueType;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
        JavaType wrapperType = property.getType();
        JavaType innerType = wrapperType.containedType(0);
        return new OptionalNullableDeserializer(innerType);
    }

    @Override
    public Optional<?> deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.getCurrentToken() == JsonToken.VALUE_NULL) {
            return Optional.empty();
        }
        Object value = context.readValue(parser, valueType);
        return Optional.ofNullable(value);
    }

    @Override
    public Optional<?> getNullValue(DeserializationContext context) {
        return null;
    }
}