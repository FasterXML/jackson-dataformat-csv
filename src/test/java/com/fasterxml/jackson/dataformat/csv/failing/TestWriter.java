package com.fasterxml.jackson.dataformat.csv.failing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.lang.String;import static org.junit.Assert.assertEquals;

public class TestWriter {

    @Test
    public void testWrite_NoNulls() throws JsonProcessingException {
        final CsvSchema.Builder csvSchemaBuilder = new CsvSchema.Builder();
        csvSchemaBuilder.addColumn("timestamp", CsvSchema.ColumnType.STRING);
        csvSchemaBuilder.addColumn("value", CsvSchema.ColumnType.NUMBER);
        csvSchemaBuilder.addColumn("id", CsvSchema.ColumnType.STRING);
        final CsvSchema schema = csvSchemaBuilder.build();
        final ObjectWriter writer = new CsvMapper().writer().withSchema(schema);

        final String string = writer.writeValueAsString(
                ImmutableMap.of("timestamp", "2014-03-10T23:32:47+00:00",
                        "value", 42, "id", "hello"));

        assertEquals("\"2014-03-10T23:32:47+00:00\",42,hello\n", string);
    }

    @Test
    public void testWrite_NullFirstColumn() throws JsonProcessingException {
        final CsvSchema.Builder csvSchemaBuilder = new CsvSchema.Builder();
        csvSchemaBuilder.addColumn("timestamp", CsvSchema.ColumnType.STRING);
        csvSchemaBuilder.addColumn("value", CsvSchema.ColumnType.NUMBER);
        csvSchemaBuilder.addColumn("id", CsvSchema.ColumnType.STRING);
        final CsvSchema schema = csvSchemaBuilder.build();
        final ObjectWriter writer = new CsvMapper().writer().withSchema(schema);

        final String string = writer.writeValueAsString(
                ImmutableMap.of("value", 42, "id", "hello"));

        assertEquals(",42,hello\n", string);
    }

    @Test
    public void testWrite_NullSecondColumn() throws JsonProcessingException {
        final CsvSchema.Builder csvSchemaBuilder = new CsvSchema.Builder();
        csvSchemaBuilder.addColumn("timestamp", CsvSchema.ColumnType.STRING);
        csvSchemaBuilder.addColumn("value", CsvSchema.ColumnType.NUMBER);
        csvSchemaBuilder.addColumn("id", CsvSchema.ColumnType.STRING);
        final CsvSchema schema = csvSchemaBuilder.build();
        final ObjectWriter writer = new CsvMapper().writer().withSchema(schema);

        final String string = writer.writeValueAsString(
                ImmutableMap.of("timestamp", "2014-03-10T23:32:47+00:00",
                        "id", "hello"));

        assertEquals("\"2014-03-10T23:32:47+00:00\",,hello\n", string);
    }

    @Test
    public void testWrite_NullThirdColumn() throws JsonProcessingException {
        final CsvSchema.Builder csvSchemaBuilder = new CsvSchema.Builder();
        csvSchemaBuilder.addColumn("timestamp", CsvSchema.ColumnType.STRING);
        csvSchemaBuilder.addColumn("value", CsvSchema.ColumnType.NUMBER);
        csvSchemaBuilder.addColumn("id", CsvSchema.ColumnType.STRING);
        final CsvSchema schema = csvSchemaBuilder.build();
        final ObjectWriter writer = new CsvMapper().writer().withSchema(schema);

        final String string = writer.writeValueAsString(
                ImmutableMap.of("timestamp", "2014-03-10T23:32:47+00:00",
                        "value", 42));

        assertEquals("\"2014-03-10T23:32:47+00:00\",42\n", string);
    }

}
