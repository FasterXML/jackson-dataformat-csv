package com.fasterxml.jackson.dataformat.csv.failing;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.*;

// Tests for [csv#56]
public class CommentsTest extends ModuleTestBase
{
    public void testSimpleComments() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        // should not be needed but seems to be...
        mapper.enable(CsvParser.Feature.WRAP_AS_ARRAY);
        final String CSV = "x,y\n# comment!\na,b\n   # another...\n";

        String[] row;
        
        // First, with comments disabled:
        
        MappingIterator<String[]> it = mapper.reader(String[].class)
                .with(mapper.schema().withComments()).readValues(CSV);

        row = it.nextValue();
        assertEquals(2, row.length);
        assertEquals("x", row[0]);
        assertEquals("y", row[1]);
        
        // next, comment visible
        assertTrue(it.hasNext());
        row = it.nextValue();
        assertEquals("# comment!", row[0]);
        assertEquals(1, row.length);

        assertTrue(it.hasNext());
        row = it.nextValue();
        assertEquals(2, row.length);
        assertEquals("a", row[0]);
        assertEquals("b", row[1]);

        assertTrue(it.hasNext());
        row = it.nextValue();
        assertEquals("   # another...", row[0]);
        assertEquals(1, row.length);

        assertFalse(it.hasNext());
        it.close();

        // Then WITH comments
        it = mapper.reader(String[].class)
                .with(mapper.schema().withComments()).readValues(CSV);

        // first row the same
        row = it.nextValue();
        assertEquals(2, row.length);
        assertEquals("x", row[0]);
        assertEquals("y", row[1]);
        
        // next, comment NOT visible
        assertTrue(it.hasNext());
        row = it.nextValue();
        assertEquals("a", row[0]);
        assertEquals(2, row.length);
        assertEquals("b", row[1]);

        // and ditto for second comment
        assertFalse(it.hasNext());
        it.close();
    }
}
