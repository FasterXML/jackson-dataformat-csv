package com.fasterxml.jackson.dataformat.csv.ser;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.fasterxml.jackson.dataformat.csv.*;

// for [dataformat-csv#57]
public class ArrayWriteTest extends ModuleTestBase
{
    @JsonPropertyOrder({"id", "values", "extra"})
    static class ValueEntry {
        public String id, extra;
        public int[] values;

        public ValueEntry(String id, String extra, int... v) {
            this.id = id;
            this.extra = extra;
            values = v;
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */
    
    public void testSimpleExplicit() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        ValueEntry input = new ValueEntry("foo", "stuff", 1, 2, 3);
        String csv = mapper.writerWithSchemaFor(ValueEntry.class)
                .writeValueAsString(input)
                .trim();
        assertEquals("foo,1;2;3,stuff", csv);
    }
}
