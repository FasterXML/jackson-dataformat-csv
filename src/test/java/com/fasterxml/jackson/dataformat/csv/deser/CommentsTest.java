package com.fasterxml.jackson.dataformat.csv.deser;

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
        CsvSchema schema = mapper.schema()
                .withComments();
        
        final String CSV = "x,y\n# comment!\na,b\n   # another!\n";

        MappingIterator<String[]> it = mapper.reader(String[].class)
                .with(schema).readValues(CSV);
        String[] row;

        row = it.nextValue();

//System.err.println("Row #1: "+Arrays.asList(row));

        assertEquals(2, row.length);
        assertEquals("x", row[0]);
        assertEquals("y", row[1]);
        
        // next, skippable comment; should be invisible
        assertTrue(it.hasNext());
        row = it.nextValue();

/*        
System.err.println("Row #2: "+Arrays.asList(row));        
        
        assertEquals(2, row.length);
        
        assertEquals("a", row[0]);
        assertEquals("b", row[1]);

        assertFalse(it.hasNext());
*/        
        it.close();
    }
}
