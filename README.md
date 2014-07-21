# Overview

[Jackson](/FasterXML/jackson) data format module for reading and writing [CSV](http://en.wikipedia.org/wiki/Comma-separated_values) encoded data, either as "raw" data (sequence of String arrays), or via data binding to/from Java Objects (POJOs).

Project is licensed under [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt).

# Status

As of version 2.3, this module is considered complete and production ready.
All Jackson layers (streaming, databind, tree model) are supported.

[![Build Status](https://fasterxml.ci.cloudbees.com/job/jackson-dataformat-csv-master/badge/icon)](https://fasterxml.ci.cloudbees.com/job/jackson-dataformat-csv-master/)

## Limitations

* Due to tabular nature of `CSV` format, deeply nested data structures are not well supported.
* Use of Tree Model (`JsonNode`) is supported, but only within limitations of `CSV`.

## Future improvements

Areas that are planned to be improved include things like:

 * Optimizations to make number handling as efficient as from JSON (but note: even with existing code, performance is typically limited by I/O and NOT parsing or generation)
 * Extensive performance measurements
 * Tests for various "exotic" CSV -- while basic escaping and quoting are tested, CSV is notoriously vaguely specified.

## Maven dependency

To use this extension on Maven-based projects, use following dependency:

```xml
<dependency>
  <groupId>com.fasterxml.jackson.dataformat</groupId>
  <artifactId>jackson-dataformat-csv</artifactId>
  <version>2.4.0</version>
</dependency>
```

# Usage

## CSV Schema: what is that?

CSV documents are essentially rows of data, instead of JSON Objects (sequences of key/value pairs).

So one potential way to expose this data is to expose a sequence of JSON arrays; and similarly allow writing of arrays.
Jackson supports this use-case (which works if you do not pass "CSV schema"), but it is not a very convenient way.

The alternative (and most commonly used) approach is to use a "CSV schema", object that defines set of names (and optionally types) for columns. This allows `CsvParser` to expose CSV data as if it was a sequence of JSON objects, name/value pairs.

## 3 ways to define Schema

So how do you get a CSV Schema instance to use? There are 3 ways:

 * Create schema based on a Java class
 * Build schema manually
 * Use the first line of CSV document to get the names (no types) for Schema

Here is code for above cases:

```java
// Schema from POJO (usually has @JsonPropertyOrder annotation)
CsvSchema schema = mapper.schemaFor(Pojo.class);

// Manually-built schema: one with type, others default to "STRING"
CsvSchema schema = CsvSchema.builder()
        .addColumn("firstName")
        .addColumn("lastName")
        .addColumn("age", CsvSchema.ColumnType.NUMBER)
        .build();

// Read schema from the first line; start with bootstrap instance
// to enable reading of schema from the first line
// NOTE: reads schema and uses it for binding
CsvSchema bootstrapSchema = CsvSchema.emptySchema().withHeader();
ObjectMapper mapper = new CsvMapper();
mapper.reader(Pojo.class).with(bootstrapSchema).readValue(json);
```

It is important to note that the schema object is needed to ensure correct ordering of columns; schema instances are immutable and fully reusable (as are `ObjectWriter` instances).

Note also that while explicit type can help efficiency it is usually not required, as Jackson data binding can do common conversions/coercions such as parsing numbers from Strings.

## Data-binding with schema

CSV content can be read either using `CsvFactory` (and parser, generators it creates) directly, or through `CsvMapper` (extension of standard `ObjectMapper`).

When using `CsvMapper`, you will be creating `ObjectReader` or `ObjectWriter` instances that pass `CsvSchema` along to `CsvParser` / `CsvGenerator`.
When creating parser/generator directly, you will need to explicitly call `setSchema(schema)` before starting to read/write content.

The most common method for reading CSV data, then, is:

```java
CsvMapper mapper = new CsvMapper();
Pojo value = ...;
CsvSchema schema = mapper.schemaFor(Pojo.class); // schema from 'Pojo' definition
String csv = mapper.writer(schema).writeValueAsString(value);
Pojo result = mapper.reader(Pojo.class).with(schema).read(csv);
```

## Data-binding without schema

But even if you do not know (or care) about column names you can read/write CSV documents. The main difference is that in this case data is exposed as a sequence of ("JSON") Arrays, not Objects, as "raw" tabular data.

So let's consider following CSV input:

```
a,b
c,d
e,f
```

By default, Jackson `CsvParser` would see it as equivalent to following JSON:

```json
["a","b"]
["c","d"]
["e","f"]
```


This is easy to use; in fact, if you ignore everything to do with Schema from above examples, you get working code. For example:

```java
CsvMapper mapper = new CsvMapper();
// important: we need "array wrapping" (see next section) here:
mapper.enable(CsvParser.Feature.WRAP_AS_ARRAY);
File csvFile = new File("input.csv"); // or from String, URL etc
MappingIterator<Object[]> it = mapper.reader(Object[].class).readValues(csvFile);
while (it.hasNext()) {
  Object[] row = it.next();
}
```

## Adding virtual Array wrapping

In addition to reading things as root-level Objects or arrays, you can also force use of virtual "array wrapping".

This means that using earlier CSV data example, parser would instead expose it similar to following JSON:

```json
[
  ["a","b"]
  ["c","d"]
  ["e","f"]
]
```

This is useful if functionality expects a single ("JSON") Array; this was the case for example when using `ObjectReader.readValues()` functionality.

# Documentation

* [Wiki](../../wiki) (includes javadocs)
* External articles:
 * [CSV with Jackson 2.0](http://www.cowtowncoder.com/blog/archives/2012/03/entry_468.html)

