package com.fasterxml.jackson.dataformat.csv.failing;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.dataformat.csv.*;

public class BrokenEncodingTest extends ModuleTestBase
{
    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    public void testLatin1asUTF8() throws Exception
    {
        CsvFactory factory = new CsvFactory();
        String CSV = "1,2\nabc,\u00A0\n";

        CsvSchema schema = CsvSchema.builder()
                .addColumn("a")
                .addColumn("b")
                .build();
        // So: take Latin-1 bytes, but construct without specifying to lead to UTF-8 handling
        CsvParser parser = factory.createParser(CSV.getBytes("ISO-8859-1"));
        parser.setSchema(schema);

        assertToken(JsonToken.START_OBJECT, parser.nextToken());
        assertToken(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals("a", parser.getCurrentName());
        assertToken(JsonToken.VALUE_STRING, parser.nextToken());
        assertToken(JsonToken.FIELD_NAME, parser.nextToken());
        assertToken(JsonToken.VALUE_STRING, parser.nextToken());
        assertEquals("2", parser.getText());
        assertToken(JsonToken.END_OBJECT, parser.nextToken());

        // problem should only be triggered now
        assertToken(JsonToken.START_OBJECT, parser.nextToken());
        assertToken(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals("a", parser.getCurrentName());
        assertToken(JsonToken.VALUE_STRING, parser.nextToken());
        assertEquals("abc", parser.getText());
        try {
            parser.nextToken();
            fail("Should trigger exception for invalid UTF-8 char");
        } catch (JsonParseException e) {
            verifyException(e, "foobar");
        }
        parser.close();
    }
}
