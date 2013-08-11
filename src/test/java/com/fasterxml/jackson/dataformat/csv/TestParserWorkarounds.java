package com.fasterxml.jackson.dataformat.csv;

import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests that verify that various minor workarounds
 * work as expected.
 */
public class TestParserWorkarounds extends ModuleTestBase
{
    /**
     * Test for [#1]; in case we get an extra empty element,
     * we can just ignore it.
     */
    public void testIgnoringOptionalTrailing() throws Exception
    {
        ObjectMapper mapper = mapperForCsv();
        CsvSchema schema = CsvSchema.builder()
            .addColumn("first")
            .addColumn("second")
            .build();

        @SuppressWarnings("resource")
        MappingIterator<Map<?,?>> it = mapper.reader(schema).withType(Map.class).readValues(
                "a,b\nc,d,\ne,f,  \nfoo,bar,x\n");
        assertTrue(it.hasNext());
        
        // first should have no problems anyway:
        Map<?,?> result = it.nextValue();
        assertEquals(2, result.size());
        assertEquals("a", result.get("first"));
        assertEquals("b", result.get("second"));

        // but second and third should skip empty trailing values
        assertTrue(it.hasNextValue());
        result = it.nextValue();
        assertEquals(2, result.size());
        assertEquals("c", result.get("first"));
        assertEquals("d", result.get("second"));

        assertTrue(it.hasNextValue());
        result = it.nextValue();
        assertEquals(2, result.size());
        assertEquals("e", result.get("first"));
        assertEquals("f", result.get("second"));

        // but then the fourth row should give an error; last entry not empty
        assertTrue(it.hasNextValue());
        try {
            result = it.nextValue();
            fail("Expected an error");
        } catch (JsonParseException e) {
            verifyException(e, "Too many entries");
        }
    }
}
