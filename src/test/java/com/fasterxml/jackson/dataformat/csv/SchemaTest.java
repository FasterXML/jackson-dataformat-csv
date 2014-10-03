package com.fasterxml.jackson.dataformat.csv;

import java.util.Collections;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

public class SchemaTest extends ModuleTestBase
{
    @JsonPropertyOrder({ "a", "b", "c", "d" })
    static class Mixed {
        public int a, b, c, d;
    }
    
    public void testSimpleWithAutoSchema() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = mapper.schemaFor(FiveMinuteUser.class);
        assertEquals("[\"firstName\",\"lastName\",\"gender\",\"verified\",\"userImage\"]", schema.getColumnDesc());
    }

    // for [Issue#42]
    public void testReorderByName() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = mapper.schemaFor(Mixed.class);
        assertEquals(aposToQuotes("['a','b','c','d']"), schema.getColumnDesc());
        schema = schema.sortedBy("b", "c");
        assertEquals(aposToQuotes("['b','c','a','d']"), schema.getColumnDesc());
    }

    // for [Issue#42]
    public void testReorderWithComparator() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = mapper.schemaFor(Mixed.class);
        schema = schema.sortedBy(Collections.<String>reverseOrder());
        assertEquals(aposToQuotes("['d','c','b','a']"), schema.getColumnDesc());
    }
}
