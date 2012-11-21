package com.fasterxml.jackson.dataformat.csv;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonToken;

import com.fasterxml.jackson.databind.MappingIterator;

public class TestParserWithHeader extends ModuleTestBase
{
    /*
    /**********************************************************************
    /* Helper types
    /**********************************************************************
     */

    @JsonPropertyOrder({ "age", "name", "cute" })
    protected static class Entry {
        public int age;
        public String name;
        public boolean cute;
    }

    /*
    /**********************************************************************
    /* Test methods, success
    /**********************************************************************
     */

    public void testSimpleHeader() throws Exception
    {
        CsvParser parser = (CsvParser) new CsvFactory().createJsonParser(
                "name, age,  other\nfoo,2,xyz\n");
        // need to enable first-line-as-schema handling:
        parser.setSchema(CsvSchema.emptySchema().withHeader());
        assertToken(JsonToken.START_OBJECT, parser.nextToken());
        CsvSchema schema = parser.getSchema();
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
        Entry entry = mapper.reader(Entry.class).with(schema).readValue(
                "name,age,\"cute\"   \nLeo,4,true\n");
        assertEquals("Leo", entry.name);
        assertEquals(4, entry.age);
        assertTrue(entry.cute);
    }

    public void testSkipFirstDataLine() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        mapper.disable(CsvParser.Feature.WRAP_AS_ARRAY);
        CsvSchema schema = mapper.schemaFor(Entry.class).withSkipFirstDataRow(true);
        MappingIterator<Entry> it = mapper.reader(Entry.class).with(schema).readValues(
                "12354\n6,Lila,true");
        Entry entry;
        
        assertTrue(it.hasNext());
        assertNotNull(entry = it.next());
        assertEquals(6, entry.age);
        assertEquals("Lila", entry.name);
        assertFalse(it.hasNext());        
    }

    public void testLongHeader() throws Exception
    {
        StringBuilder sb = new StringBuilder(650);
        ArrayList<String> names = new ArrayList<String>();
        
        do {
            if (sb.length() > 0) {
                sb.append(',');
            }
            String name = "COLUMN"+names.size();
            names.add(name);
            sb.append(name);
        } while (sb.length() < 600);
        sb.append("\nabc\n");
        final String CSV = sb.toString();


        // Ok, then, first let's try reading columns:        
        
        CsvMapper mapper = mapperForCsv();
        mapper.disable(CsvParser.Feature.WRAP_AS_ARRAY);
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        CsvParser p = (CsvParser) mapper.getFactory().createJsonParser(CSV);
        p.setSchema(schema);
        // need to read something to ensure header line is processed
        assertEquals(JsonToken.START_OBJECT, p.nextToken());
        CsvSchema actual = p.getSchema();
        
        assertEquals(names.size(), actual.size());
        for (int i = 0, len = names.size(); i < len; ++i) {
            CsvSchema.Column col = actual.column(i);
            assertEquals(names.get(i), col.getName());
        }
        p.close();
    }

    public void testLongColumnName() throws Exception
    {
        StringBuilder sb = new StringBuilder(650);

        sb.append("COLUMN");
        
        for (int i = 0; i < 600; ++i) {
            sb.append((char) ('0' + i%10));
        }
        final String COLUMN = sb.toString();
        sb.append("\nabc\n");
        final String CSV = sb.toString();

        // Ok, then, first let's try reading columns:        
        
        CsvMapper mapper = mapperForCsv();
        mapper.disable(CsvParser.Feature.WRAP_AS_ARRAY);
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        CsvParser p = (CsvParser) mapper.getFactory().createJsonParser(CSV);
        p.setSchema(schema);
        // need to read something to ensure header line is processed
        assertEquals(JsonToken.START_OBJECT, p.nextToken());
        CsvSchema actual = p.getSchema();
        
        assertEquals(1, actual.size());
        assertEquals(COLUMN, actual.column(0).getName());
        p.close();
    }
    
    /*
    /**********************************************************************
    /* Test methods, fail
    /**********************************************************************
     */

    public void testInvalidMissingHeader() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        try {
            mapper.reader(Entry.class).with(CsvSchema.emptySchema().withHeader()).readValue("  \nJoseph,57,false");
            fail("Should have failed with exception");
        } catch (Exception e) {
            verifyException(e, "Empty header line");
        }
    }

}
