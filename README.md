# Overview

This projects aims at adding [Jackson](http://http://wiki.fasterxml.com/JacksonHome) extension component to allow reading and writing [CSV](http://en.wikipedia.org/wiki/Comma-separated_values) encoded data.

# Status

Implementation has started, and currently following features work:

 * Basic writing of CSV and serializating POJOs
 * Basic reading of CSV and deserializating POJOs
 * Construction of `CsvSchema` instances using `CsvMapper` (schemas are needed for reading and writing)
 * Reading `CsvSchema` from the first line of CSV document (just names)

What is missing:

 * Optimizations to make number handling as efficient as from JSON
 * Measurements, performance
 * Extensive tests for various "exotic" CSV

# Usage

## Data-binding with explicit "schema"

CSV content can be read either using `CsvFactory` (and parser, generators it creates) directly, or through `CsvMapper` (extension of standard `ObjectMapper`).

The expected main use case is that of data-binding, since it is easier to deal with requirement of passing a `CsvSchema` instance: `CsvMapper` has methods for constructing these from POJOs (pass in `Class`, `TypeReference` or `JavaType`):

    CsvMapper mapper = new CsvMapper();
    Pojo value = ...;
    CsvSchema schema = mapper.schemaFor(Pojo.class); // needed to know ordering
    String csv = mapper.writer(schema).writeValueAsString();

It is important to note that the schema object is needed to ensure correct ordering of columns; schema instances are immutable and fully reusable (as are `ObjectWriter` instances).

It is also possible to manually create schema instance:

    CsvSchema schema = CsvSchema.builder()
            .addColumn("firstName")
            .addColumn("lastName")
            .addColumn("age", CsvSchema.ColumnType.NUMBER)
            .build();

which would create a CSV schema with 3 columns, first type with the default type (CsvSchema.ColumnType.STRING), and last with numeric type.

Note that while explicit type can help efficiency it is usually not required, as Jackson data binding can do common conversions/coercions such as parsing numbers from Strings.

## Data-binding without schema

But even if you do not know (or care) about column names you can read/write CSV documents. The main difference is that in this case data is exposed as a sequence of "JSON" Arrays, not Objects.

(NEED TO ADD AN EXAMPLE)
