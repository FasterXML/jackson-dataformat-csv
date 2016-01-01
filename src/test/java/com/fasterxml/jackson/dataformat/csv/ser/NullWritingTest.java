package com.fasterxml.jackson.dataformat.csv.ser;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.csv.*;

// for [dataformat-csv#69], other null value serialization
public class NullWritingTest extends ModuleTestBase
{
    private final CsvMapper csv = new CsvMapper();
    
    public static class Nullable {
        public String a, b, c, d;
    }
     
    public void testObjectWithNullMembersToString() throws Exception {
        CsvSchema schema = csv.schemaFor(Nullable.class).withUseHeader(true);
        ObjectWriter writer = csv.writer(schema);
        String nullMembers = writer.writeValueAsString(new Nullable());    
        assertEquals("a,b,c,d\n,,,\n", nullMembers);
    }

    public void testNullToString() throws Exception {
        CsvSchema schema = csv.schemaFor(Nullable.class).withUseHeader(true);
        ObjectWriter writer = csv.writer(schema);
        String nullObject = writer.writeValueAsString(null);
        assertEquals("a,b,c,d\n", nullObject);
    }

    public void testObjectWithNullMembersToStream() throws Exception {
        CsvSchema schema = csv.schemaFor(Nullable.class).withUseHeader(true);
        ObjectWriter writer = csv.writer(schema);

        // Write an object with null members
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        SequenceWriter writeValues = writer.writeValues(stream);
        writeValues.write(new Nullable());
        writeValues.write(new Nullable());
        writeValues.flush();
        String nullMembers = stream.toString("UTF-8");
        assertEquals("a,b,c,d\n,,,\n,,,\n", nullMembers);
        writeValues.close();
    }

    public void testNullToStream() throws Exception {
        CsvSchema schema = csv.schemaFor(Nullable.class).withUseHeader(true);
        ObjectWriter writer = csv.writer(schema);
         
        // Write a null value
        StringWriter sw = new StringWriter();
        SequenceWriter writeValues = writer.writeValues(sw);
        writeValues.write(null);
        writeValues.write(null);
        writeValues.flush();
        String nullObject = sw.toString();
        /* 11-Feb-2015, tatu: Two ways to go; either nulls get ignored, or they trigger serialization of
         *   empty Object. For now, former occurs:
         */
        
        assertEquals("a,b,c,d\n", nullObject);
//        assertEquals("a,b,c,d\n\n\n", nullObject);
        writeValues.close();
    }

    // [dataformat-csv#53]
    public void testCustomNullValue() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = CsvSchema.builder()
                .setNullValue("n/a")
                .addColumn("id")
                .addColumn("desc")
                .build();
        String result = mapper.writer(schema).writeValueAsString(new IdDesc("id", null));
        // MUST use doubling for quotes!
        assertEquals("id,n/a\n", result);
    }

    public void testCustomNullValueInObjectListIssue106() throws Exception
    {
        ObjectMapper mapper = mapperForCsv();
        CsvSchema schema = CsvSchema.builder()
            .setArrayElementSeparator(",")
            .setNullValue("n/a")
            .setAllowNullFields(true)
            .build();

        List list = Arrays.asList("d0", null, "d2");

        String result = mapper.writer(schema).writeValueAsString(list);
        assertEquals("d0,n/a,d2\n", result);

        list = Arrays.asList(null, "d1", "d2");
        result = mapper.writer(schema).writeValueAsString(list);
        assertEquals("n/a,d1,d2\n", result);

        list = Arrays.asList("d0", "d1", null);
        result = mapper.writer(schema).writeValueAsString(list);
        assertEquals("d0,d1,n/a\n", result);
    }

    public void testDefaultNullValueInObjectListIssue106() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = CsvSchema.builder()
            .setArrayElementSeparator(",")
            .setAllowNullFields(true)
            .build();

        List list = Arrays.asList("d0", null, "d2");
        String result = mapper.writer(schema).writeValueAsString(list);
        assertEquals("d0,,d2\n", result);

        list = Arrays.asList(null, "d1", "d2");
        result = mapper.writer(schema).writeValueAsString(list);
        assertEquals(",d1,d2\n", result);

        list = Arrays.asList("d0", "d1", null);
        result = mapper.writer(schema).writeValueAsString(list);
        assertEquals("d0,d1,\n", result);
    }
}
