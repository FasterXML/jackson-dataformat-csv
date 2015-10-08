package com.fasterxml.jackson.dataformat.csv.deser;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MappingIterator;

import com.fasterxml.jackson.dataformat.csv.*;

public class SequenceRecoveryTest extends ModuleTestBase
{
    @JsonPropertyOrder({"x", "y"})
    protected static class Entry {
        public int x, y;

        public Entry() { }
        public Entry(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public boolean equals(Entry e) { // simplified just for testing
            return e.x == this.x && e.y == this.y;
        }

        @Override
        public String toString() {
            return "["+x+","+y+"]";
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    public void testSequenceRecovery() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        mapper.disable(CsvParser.Feature.WRAP_AS_ARRAY);
        MappingIterator<Entry> it = mapper.readerWithSchemaFor(Entry.class).readValues(
                "1,2\n3,invalid\n5,6\n1,2,3,5\n13,-4\ngarbage\n");
        Entry entry;
        
        assertTrue(it.hasNext());
        assertNotNull(entry = it.nextValue());
        assertEquals(1, entry.x);
        assertEquals(2, entry.y);
        assertTrue(it.hasNext());

        // second row, invalid:
        try {
            it.nextValue();
            fail("Shouldn't have passed");
        } catch (JsonMappingException e) {
            verifyException(e, "'invalid': not a valid");
        }

        // but third is fine again
        assertNotNull(entry = it.nextValue());
        assertEquals(5, entry.x);
        assertEquals(6, entry.y);

        // fourth not
        assertTrue(it.hasNext());
        try {
            it.nextValue();
            fail("Shouldn't have passed");
        } catch (JsonProcessingException e) {
            // !!! TODO, maybe: Would be nicer to get a JsonMappingException?
            verifyException(e, "Too many entries");
        }

        // fifth ok
        assertTrue(it.hasNext());
        assertNotNull(entry = it.nextValue());
        assertEquals(13, entry.x);
        assertEquals(-4, entry.y);

        // and sixth busted again
        assertTrue(it.hasNext());
        try {
            it.nextValue();
            fail("Shouldn't have passed");
        } catch (JsonMappingException e) {
            verifyException(e, "String value 'garbage'");
        }
        assertFalse(it.hasNext());
        it.close();
    }

    // for [dataformat-csv#91]: ensure recovery works for extra columns
    public void testRecoverFromExtraColumns91() throws Exception
    {
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = mapper.schemaFor(Entry.class);
        final String CSV = "1,2\n3,4,\n5,6\n7,8,,foo,\n9,10\n";
        MappingIterator<Entry> it = mapper.readerFor(Entry.class)
                .with(schema)
                .readValues(CSV);
        Entry entry;

        assertTrue(it.hasNext());
        entry = it.nextValue();
        assertNotNull(entry);
        assertEquals(1, entry.x);
        assertEquals(2, entry.y);

        // one extra empty column always allowed
        assertTrue(it.hasNext());
        entry = it.nextValue();
        assertEquals(3, entry.x);
        assertEquals(4, entry.y);

        assertTrue(it.hasNext());
        entry = it.nextValue();
        assertEquals(5, entry.x);
        assertEquals(6, entry.y);

        assertTrue(it.hasNext());
        try {
            entry = it.nextValue();
            fail("Should fail");
        } catch (JsonProcessingException e) {
            verifyException(e, "Too many entries");
        }
        // this SHOULD skip 7,8,, entry

        assertTrue(it.hasNext());
        entry = it.nextValue();
        assertEquals(9, entry.x);
        assertEquals(10, entry.y);

        assertFalse(it.hasNext());

        it.close();
    }

    // for [dataformat-csv#91]: ensure recovery works for extra columns
    public void testRecoverFromMissingQuote91() throws Exception
    {
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = mapper.schemaFor(IdDesc.class);
        final String CSV = "a,\"desc\"\nb,\"Broken\nc,\"good\"\nd,foo";
        MappingIterator<IdDesc> it = mapper.readerFor(IdDesc.class)
                .with(schema)
                .readValues(CSV);
        IdDesc value;

        assertTrue(it.hasNext());
        value = it.nextValue();
        assertEquals("a", value.id);
        assertEquals("desc", value.desc);

        
        // Should choke on 'g', because it's not a separator after what parser
        // thinks is closing quote (but user probably meant as opening)
        assertTrue(it.hasNext());
        try {
            value = it.nextValue();
            fail("Should fail");
        } catch (JsonProcessingException e) {
            verifyException(e, "Expected separator");
        }

        // and because of this, synchronization skips the rest of 3rd line...
        assertTrue(it.hasNext());
        value = it.nextValue();
        assertEquals("d", value.id);
        assertEquals("foo", value.desc);

        it.close();
    }
}
