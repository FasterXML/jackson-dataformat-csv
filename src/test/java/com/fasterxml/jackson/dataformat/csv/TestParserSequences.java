package com.fasterxml.jackson.dataformat.csv;

import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.MappingIterator;

/**
 * Tests for verifying behavior of enclosing input stream as
 * a logical array.
 */
public class TestParserSequences extends ModuleTestBase
{
    /*
    /**********************************************************************
    /* Helper types
    /**********************************************************************
     */

    @JsonPropertyOrder({"x", "y"})
    protected static class Entry {
        public int x, y;
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    /**
     * Test using non-wrapped sequence of entries
     */
    public void testAsSequence() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        mapper.disable(CsvParser.Feature.WRAP_AS_ARRAY);
        MappingIterator<Entry> it = mapper.readerWithSchemaFor(Entry.class).readValues(
                "1,2\n-3,0\n5,6\n");
        Entry entry;
        
        assertTrue(it.hasNext());
        assertNotNull(entry = it.next());
        assertEquals(1, entry.x);
        assertEquals(2, entry.y);
        assertTrue(it.hasNext());
        assertNotNull(entry = it.next());
        assertEquals(-3, entry.x);
        assertEquals(0, entry.y);
        assertTrue(it.hasNext());
        assertNotNull(entry = it.next());
        assertEquals(5, entry.x);
        assertEquals(6, entry.y);
        assertFalse(it.hasNext());
    }

    /**
     * Test using sequence of entries wrapped in a logical array.
     */
    public void testAsWrappedArray() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        mapper.enable(CsvParser.Feature.WRAP_AS_ARRAY);
        Entry[] entries = mapper.readerWithSchemaFor(Entry.class).withType(Entry[].class)
                .readValue("1,2\n0,0\n123,123456789\n");
        assertEquals(3, entries.length);
        assertEquals(1, entries[0].x);
        assertEquals(2, entries[0].y);
        assertEquals(0, entries[1].x);
        assertEquals(0, entries[1].y);
        assertEquals(123, entries[2].x);
        assertEquals(123456789, entries[2].y);
    }
}
