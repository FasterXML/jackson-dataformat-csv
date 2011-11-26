package com.fasterxml.jackson.dataformat.csv;

import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.MappingIterator;

public class TestParserQuotes extends ModuleTestBase
{
    /*
    /**********************************************************************
    /* Helper types
    /**********************************************************************
     */
    @JsonPropertyOrder({"age, name"})
    protected static class AgeName {
        public int age;
        public String name;
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    public void testSimpleQuotes() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = mapper.schemaFor(AgeName.class);
        AgeName user = mapper.reader(schema).withType(AgeName.class).readValue(" 13  ,\"Joe \"\"Sixpack\"\" Paxson\"");
        assertEquals(13, user.age);
        assertEquals("Joe \"Sixpack\" Paxson", user.name);
    }

    public void testSimpleMultiLine() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        mapper.disable(CsvParser.Feature.WRAP_AS_ARRAY);
        CsvSchema schema = mapper.schemaFor(AgeName.class);
        MappingIterator<AgeName> it = mapper.reader(schema).withType(AgeName.class).readValues(
                "-3,\"\"\"Unknown\"\"\"\n13  ,\"Joe \"\"Sixpack\"\" Paxson\"");
        assertTrue(it.hasNext());
        AgeName user = it.nextValue();
        assertEquals(-3, user.age);
        assertEquals("\"Unknown\"", user.name);
        assertTrue(it.hasNext());
        user = it.nextValue();
        assertEquals(13, user.age);
        assertEquals("Joe \"Sixpack\" Paxson", user.name);
        assertFalse(it.hasNext());
    }
}
