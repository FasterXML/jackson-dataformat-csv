package com.fasterxml.jackson.dataformat.csv.deser;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

/**
 * Tests for cases where one more of schema-declared columns is
 * missing.
 */
public class MissingColumnsTest extends ModuleTestBase
{
    @JsonPropertyOrder({ "a", "b", "c", "d" })
    static class ABCD {
        public String a = "a";
        public String b = "b";
        public String c = "c";
        public String d = "d";
    }
    
    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    final CsvMapper MAPPER = mapperForCsv();

    final CsvSchema schema = MAPPER.schemaFor(ABCD.class);

    // [dataformat-csv#137]: inject `null`s in place of missing
    public void testInjectMissingAsNulls() throws Exception
    {
        
        ObjectReader r = MAPPER.readerFor(ABCD.class)
                .with(schema)
                .with(CsvParser.Feature.INSERT_NULLS_FOR_MISSING_COLUMNS);
        
        // check with various number of missing; but first with no missing
        ABCD result = r.readValue("first,second,third,fourth\n");
        assertEquals("third", result.c);
        assertEquals("fourth", result.d);

        // then with one missing
        result = r.readValue("first,second,third\n");
        assertEquals("third", result.c);
        assertNull(result.d);

        // two
        result = r.readValue("first,second\n");
        assertEquals("second", result.b);
        assertNull(result.c);
        assertNull(result.d);

        // etc
        result = r.readValue("first\n");
        assertEquals("first", result.a);
        assertNull(result.b);
        assertNull(result.c);
        assertNull(result.d);

        result = r.readValue("\n");
        // 16-Mar-2017, tatu: Actually first value is just empty, not null... since
        //   logical "empty String" does exist no matter what.
        assertEquals("", result.a);
        assertNull(result.b);
        assertNull(result.c);
        assertNull(result.d);
    }
}
