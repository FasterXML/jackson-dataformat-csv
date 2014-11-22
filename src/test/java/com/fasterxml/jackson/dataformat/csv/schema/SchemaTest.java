package com.fasterxml.jackson.dataformat.csv.schema;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;
import com.fasterxml.jackson.dataformat.csv.CsvSchema.Column;

public class SchemaTest extends ModuleTestBase
{
    @JsonPropertyOrder({ "a", "b", "c", "d" })
    static class Mixed {
        public int a, b, c, d;
    }

    @JsonPropertyOrder({ "a", "b", "c" })
    static class ArrayWrapper {
        public int[] a;
        public int b;
        public List<String> c;
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    public void testUserWithTypedAutoSchema() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = mapper.typedSchemaFor(FiveMinuteUser.class);
        assertEquals("[\"firstName\",\"lastName\",\"gender\",\"verified\",\"userImage\"]",
                schema.getColumnDesc());
        assertEquals(5, schema.size());
        Iterator<Column> it = schema.iterator();
        CsvSchema.Column col;

        col = it.next();
        assertEquals("firstName", col.getName());
        assertEquals(CsvSchema.ColumnType.STRING, col.getType());
        col = it.next();
        assertEquals("lastName", col.getName());
        assertEquals(CsvSchema.ColumnType.STRING, col.getType());
        col = it.next();
        assertEquals("gender", col.getName());
        assertEquals(CsvSchema.ColumnType.NUMBER_OR_STRING, col.getType());
        col = it.next();
        assertEquals("verified", col.getName());
        assertEquals(CsvSchema.ColumnType.BOOLEAN, col.getType());
        col = it.next();
        assertEquals("userImage", col.getName());
        assertEquals(CsvSchema.ColumnType.STRING, col.getType());
        assertFalse(it.hasNext());
    }

    public void testArrayWithTypedAutoSchema() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = mapper.typedSchemaFor(ArrayWrapper.class);
        assertEquals("[\"a\",\"b\",\"c\"]",
                schema.getColumnDesc());
        assertEquals(3, schema.size());
        Iterator<Column> it = schema.iterator();
        CsvSchema.Column col;

        col = it.next();
        assertEquals("a", col.getName());
        assertEquals(CsvSchema.ColumnType.ARRAY, col.getType());
        col = it.next();
        assertEquals("b", col.getName());
        assertEquals(CsvSchema.ColumnType.NUMBER, col.getType());
        col = it.next();
        assertEquals("c", col.getName());
        // List:
        assertEquals(CsvSchema.ColumnType.ARRAY, col.getType());
        assertFalse(it.hasNext());
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
