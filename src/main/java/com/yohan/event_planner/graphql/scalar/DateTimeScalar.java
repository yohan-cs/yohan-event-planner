package com.yohan.event_planner.graphql.scalar;

import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateTimeScalar {

    public static final GraphQLScalarType DATE_TIME = GraphQLScalarType.newScalar()
            .name("DateTime")
            .description("ISO-8601 compliant date-time scalar with timezone offset, represented as java.time.ZonedDateTime")
            .coercing(new Coercing<ZonedDateTime, String>() {

                @Override
                public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
                    if (dataFetcherResult instanceof ZonedDateTime zdt) {
                        return zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                    }
                    throw new CoercingSerializeException("Expected a ZonedDateTime object.");
                }

                @Override
                public ZonedDateTime parseValue(Object input) throws CoercingParseValueException {
                    try {
                        if (input instanceof String str) {
                            return ZonedDateTime.parse(str, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                        }
                        throw new CoercingParseValueException("Expected a String");
                    } catch (DateTimeParseException e) {
                        throw new CoercingParseValueException("Invalid ISO-8601 DateTime value: " + input);
                    }
                }

                @Override
                public ZonedDateTime parseLiteral(Object input) throws CoercingParseLiteralException {
                    if (input instanceof StringValue sv) {
                        try {
                            return ZonedDateTime.parse(sv.getValue(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                        } catch (DateTimeParseException e) {
                            throw new CoercingParseLiteralException("Invalid ISO-8601 DateTime literal: " + sv.getValue());
                        }
                    }
                    throw new CoercingParseLiteralException("Expected AST type 'StringValue'");
                }
            })
            .build();
}
