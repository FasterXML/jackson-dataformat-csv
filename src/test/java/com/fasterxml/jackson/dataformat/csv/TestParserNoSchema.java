package com.fasterxml.jackson.dataformat.csv;

import java.util.List;

import com.fasterxml.jackson.databind.MappingIterator;

/**
 * Test to verify that CSV content can be parsed without schema
 * (or more precisely: using Schema that does not define any columns);
 * if so, content will be exposed as a sequence of JSON Arrays, instead
 * of JSON Objects.
 */
public class TestParserNoSchema extends ModuleTestBase
{
    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    public void testUntypedAsSequence() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        mapper.disable(CsvParser.Feature.WRAP_AS_ARRAY);

        /* 04-Oct-2012, tatu: Due to changes to 2.1, this is the one case
         *   that does NOT work automatically via ObjectMapper/-Reader, but
         *   instead we must manually create the reader
         */
        final String CSV = "1,null\nfoobar\n7,true\n";
        CsvParser cp = mapper.getFactory().createParser(CSV);
        
        MappingIterator<Object[]> it = mapper.reader(Object[].class).readValues(cp);

        Object[] row;
        assertTrue(it.hasNext());
        row = it.next();
        assertEquals(2, row.length);
        assertEquals("1", row[0]);
        assertEquals("null", row[1]);

        assertTrue(it.hasNext());
        row = it.next();
        assertEquals(1, row.length);
        assertEquals("foobar", row[0]);

        assertTrue(it.hasNext());
        row = it.next();
        assertEquals(2, row.length);
        assertEquals("7", row[0]);
        assertEquals("true", row[1]);

        assertFalse(it.hasNext());

        cp.close();
    }
    
    public void testUntypedAsArray() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        mapper.enable(CsvParser.Feature.WRAP_AS_ARRAY);
        // when wrapped as an array, we'll get array of Lists:
        Object[] rows = mapper.reader(Object[].class).readValue(
            "1,\"xyz\"\n\ntrue,\n"
                );
        assertEquals(3, rows.length);
        List<?> row;

        row = (List<?>) rows[0];
        assertEquals(2, row.size());
        assertEquals("1", row.get(0));
        assertEquals("xyz", row.get(1));

        row = (List<?>) rows[1];
        assertEquals(1, row.size());
        assertEquals("", row.get(0));

        row = (List<?>) rows[2];
        assertEquals(2, row.size());
        assertEquals("true", row.get(0));
        assertEquals("", row.get(1));
    }

    /* Let's also allow varying number of columns, if no
     * schema has been defined.
     */
    public void testUntypedAsSequenceVarLengths() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        mapper.disable(CsvParser.Feature.WRAP_AS_ARRAY);

        /* 04-Oct-2012, tatu: Due to changes to 2.1, this is the one case
         *   that does NOT work automatically via ObjectMapper/-Reader, but
         *   instead we must manually create the reader
         */
        final String CSV = "1,2\n1,2,3,4\n";
        CsvParser cp = mapper.getFactory().createParser(CSV);
        
        MappingIterator<Object[]> it = mapper.reader(Object[].class).readValues(cp);

        Object[] row;
        assertTrue(it.hasNext());
        row = it.next();
        assertEquals(2, row.length);
        assertEquals("1", row[0]);
        assertEquals("2", row[1]);

        assertTrue(it.hasNext());
        row = it.next();
        assertEquals(4, row.length);
        assertEquals("1", row[0]);
        assertEquals("2", row[1]);
        assertEquals("3", row[2]);
        assertEquals("4", row[3]);

        assertFalse(it.hasNext());

        cp.close();
    }

}
