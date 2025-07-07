package com.yohan.event_planner.graphql.scalar;

import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateScalar {

    public static final GraphQLScalarType DATE = GraphQLScalarType.newScalar()
            .name("Date")
            .description("An ISO-8601 compliant date scalar without time (e.g., 2025-06-23), represented as java.time.LocalDate")
            .coercing(new Coercing<LocalDate, String>() {

                @Override
                public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
                    if (dataFetcherResult instanceof LocalDate date) {
                        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
                    }
                    throw new CoercingSerializeException("Expected a LocalDate object.");
                }

                @Override
                public LocalDate parseValue(Object input) throws CoercingParseValueException {
                    try {
                        if (input instanceof String str) {
                            return LocalDate.parse(str, DateTimeFormatter.ISO_LOCAL_DATE);
                        }
                        throw new CoercingParseValueException("Expected a String");
                    } catch (DateTimeParseException e) {
                        throw new CoercingParseValueException("Invalid ISO-8601 Date value: " + input);
                    }
                }

                @Override
                public LocalDate parseLiteral(Object input) throws CoercingParseLiteralException {
                    if (input instanceof StringValue sv) {
                        try {
                            return LocalDate.parse(sv.getValue(), DateTimeFormatter.ISO_LOCAL_DATE);
                        } catch (DateTimeParseException e) {
                            throw new CoercingParseLiteralException("Invalid ISO-8601 Date literal: " + sv.getValue());
                        }
                    }
                    throw new CoercingParseLiteralException("Expected AST type 'StringValue'");
                }
            })
            .build();
}