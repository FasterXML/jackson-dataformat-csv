package com.fasterxml.jackson.dataformat.csv;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;

public class GeneratorIgnoreUnknownTest extends ModuleTestBase
{
    @JsonPropertyOrder({ "x", "y", "z" })
    public static class Point {
        public int x;
        public Integer y;
        public Integer z = 8;
    }    

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */    

    public void testSimpleIgnore() throws Exception
    {
        ObjectMapper mapper = mapperForCsv();
        final CsvSchema schema = CsvSchema.builder()
                .addColumn("x")
                .addColumn("z")
                .build();
        ObjectWriter writer = mapper.writerFor(Point.class)
                .with(schema)
                .with(JsonGenerator.Feature.IGNORE_UNKNOWN);
        String csv = writer.writeValueAsString(new Point());
        assertNotNull(csv);
    }
}
