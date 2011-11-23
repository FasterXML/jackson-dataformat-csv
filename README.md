# Overview

This projects aims at adding [Jackson](http://http://wiki.fasterxml.com/JacksonHome) extension component to allow reading and writing [CSV](http://en.wikipedia.org/wiki/Comma-separated_values) encoded data.

# Status

Implementation has started, and currently following features work:

 * Basic writing of CSV and serialization POJOs! (although note that handling of CSV quoting has not been well tested yet)
 * Construction of `CsvSchema` instances using `CsvMapper` (schemas are needed for reading and writing)

What is missing:

 * Reading (deserialization)

# Usage

CSV content can be read either using `CsvFactory` (and parser, generators it creates) directly, or through `CsvMapper` (extension of standard `ObjectMapper`).

The expected main use case is that of data-binding, since it is easier to deal with requirement of passing a `CsvSchema` instance: `CsvMapper` has methods for constructing these from POJOs (pass in `Class`, `TypeReference` or `JavaType`):

    CsvMapper mapper = new CsvMapper();
    Pojo value = ...;
    CsvSchema schema = mapper.schemaFor(Pojo.class); // needed to know ordering
    String csv = mapper.writer(schema).writeValueAsString();

It is important to note that the schema object is needed to ensure correct ordering of columns; schema instances are immutable and fully reusable (as are `ObjectWriter` instances).

