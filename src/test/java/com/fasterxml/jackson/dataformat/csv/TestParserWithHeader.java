package com.fasterxml.jackson.dataformat.csv;

import org.codehaus.jackson.JsonToken;

public class TestParserWithHeader extends ModuleTestBase
{
    /*
    /**********************************************************************
    /* Helper types
    /**********************************************************************
     */

    protected static class Entry {
        public int age;
        public String name;
        public boolean cute;
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    public void testSimpleHeader() throws Exception
    {
        CsvParser parser = (CsvParser) new CsvFactory().createJsonParser(
                "name, age,  other\nfoo,2,xyz\n");
        // need to enable first-line-as-schema handling:
        parser.setSchema(CsvSchema.emptySchema().withHeader());
        assertToken(JsonToken.START_OBJECT, parser.nextToken());
        CsvSchema schema = (CsvSchema) parser.getSchema();
        assertEquals(3, schema.size());

        // verify that names from first line are trimmed:
        assertEquals("name", schema.column(0).getName());
        assertEquals("age", schema.column(1).getName());
        assertEquals("other", schema.column(2).getName());
    }
    
    public void testSimpleQuotes() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        mapper.disable(CsvParser.Feature.WRAP_AS_ARRAY);
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        Entry entry = mapper.reader(Entry.class).withSchema(schema).readValue(
                "name,age,\"cute\"   \nLeo,4,true\n");
        assertEquals("Leo", entry.name);
        assertEquals(4, entry.age);
        assertTrue(entry.cute);
    }

    public void testInvalidMissingHeader() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        try {
            mapper.reader(Entry.class).withSchema(CsvSchema.emptySchema().withHeader()).readValue("  \nJoseph,57,false");
            fail("Should have failed with exception");
        } catch (Exception e) {
            verifyException(e, "Empty header line");
        }
    }
}
