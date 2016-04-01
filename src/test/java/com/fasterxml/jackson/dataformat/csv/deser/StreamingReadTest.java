package com.fasterxml.jackson.dataformat.csv.deser;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.dataformat.csv.*;

/**
 * Container for some low-level tests that use parser directly;
 * needed for exercising certain methods that are difficult to
 * trigger via data-binding
 */
public class StreamingReadTest extends ModuleTestBase
{
    public void testNumbers() throws Exception
    {
        CsvFactory factory = new CsvFactory();
        String CSV = "111,16.25,-123456789\n";

        CsvSchema schema = CsvSchema.builder()
                .addColumn("a")
                .addColumn("b")
                .addColumn("c")
                .setUseHeader(false)
                .build();
        CsvParser parser = factory.createParser(CSV);
        parser.setSchema(schema);

        assertToken(JsonToken.START_OBJECT, parser.nextToken());

        assertToken(JsonToken.FIELD_NAME, parser.nextToken());
        assertEquals("a", parser.getCurrentName());
        assertEquals("111", parser.nextTextValue());
        assertEquals(111L, parser.getLongValue());

        assertEquals("b", parser.nextFieldName());

        assertEquals("16.25", parser.nextTextValue());
        assertEquals(16.25, parser.getDoubleValue());

        assertTrue(parser.nextFieldName(new SerializedString("c")));

        assertToken(JsonToken.VALUE_STRING, parser.nextToken());
        assertEquals(-123456789, parser.getIntValue());

        assertToken(JsonToken.END_OBJECT, parser.nextToken());
        assertNull(parser.nextToken());
        
        parser.close();
    }
}
