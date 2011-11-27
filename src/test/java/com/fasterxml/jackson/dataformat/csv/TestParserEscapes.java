package com.fasterxml.jackson.dataformat.csv;

import org.codehaus.jackson.annotate.JsonPropertyOrder;

public class TestParserEscapes extends ModuleTestBase
{
    /*
    /**********************************************************************
    /* Helper types
    /**********************************************************************
     */
    @JsonPropertyOrder({"id", "description"})
    protected static class Desc {
        public String id, desc;
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    public void testSimpleEscapes() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = mapper.schemaFor(Desc.class).withColumnSeparator('|').withEscapeChar('\\');
        final String id = "abc\\\\def"; // doubled for javac
        final String desc = "Desc with\\\nlinefeed";
        String input = quote(id)+"|"+quote(desc)+"\n";
        Desc result = mapper.reader(schema).withType(Desc.class).readValue(input);
        assertEquals("abc\\def", result.id);
        assertEquals("Desc with\nlinefeed", result.desc);
    }

}
