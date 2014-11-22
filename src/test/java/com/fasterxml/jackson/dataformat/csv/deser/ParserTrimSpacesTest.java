package com.fasterxml.jackson.dataformat.csv;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.fasterxml.jackson.databind.MappingIterator;

public class ParserTrimSpacesTest extends ModuleTestBase
{
    /*
    /**********************************************************************
    /* Helper types
    /**********************************************************************
     */

    @JsonPropertyOrder({"a", "b", "c"})
    protected static class Entry {
        public String a, b, c;
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    // Test to verify default behavior of not trimming spaces
    public void testNonTrimming() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        mapper.disable(CsvParser.Feature.TRIM_SPACES);
        MappingIterator<Entry> it = mapper.readerWithSchemaFor(Entry.class).readValues(
                "a,  b,  c  \n 1,2,\"3 \"\n"
                );
        Entry entry;
        
        assertTrue(it.hasNext());
        assertNotNull(entry = it.nextValue());
        assertEquals("a", entry.a);
        assertEquals("  b", entry.b);
        assertEquals("  c  ", entry.c);

        assertTrue(it.hasNext());
        assertNotNull(entry = it.nextValue());
        assertEquals(" 1", entry.a);
        assertEquals("2", entry.b);
        assertEquals("3 ", entry.c);

        assertFalse(it.hasNext());
        it.close();
    }

    public void testTrimming() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        mapper.enable(CsvParser.Feature.TRIM_SPACES);
        MappingIterator<Entry> it = mapper.readerWithSchemaFor(Entry.class).readValues(
                "a,  b,  c\t\n 1,2,\" 3\" \n\"ab\t\" ,\"c\",  \n"
                );
        Entry entry;
        
        assertTrue(it.hasNext());
        assertNotNull(entry = it.nextValue());
        assertEquals("a", entry.a);
        assertEquals("b", entry.b);
        assertEquals("c", entry.c);

        assertTrue(it.hasNext());
        assertNotNull(entry = it.nextValue());
        assertEquals("1", entry.a);
        assertEquals("2", entry.b);
        assertEquals(" 3", entry.c); // note: space within quotes is preserved

        assertTrue(it.hasNext());
        assertNotNull(entry = it.nextValue());
        assertEquals("ab\t", entry.a);
        assertEquals("c", entry.b);
        assertEquals("", entry.c);
        
        assertFalse(it.hasNext());
        it.close();
    }
}
