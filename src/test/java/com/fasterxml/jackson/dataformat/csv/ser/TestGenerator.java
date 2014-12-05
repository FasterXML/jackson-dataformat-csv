package com.fasterxml.jackson.dataformat.csv.ser;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.csv.CsvGenerator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

import java.io.File;

public class TestGenerator extends ModuleTestBase
{
    @JsonPropertyOrder({"id", "amount"})
    static class Entry {
        public String id;
        public double amount;

        public Entry(String id, double amount) {
            this.id = id;
            this.amount = amount;
        }
    }

    @JsonPropertyOrder({"id", "amount"})
    static class Entry2 {
        public String id;
        public float amount;

        public Entry2(String id, float amount) {
            this.id = id;
            this.amount = amount;
        }
    }

    @JsonPropertyOrder({"id", "desc"})
    static class IdDesc {
        public String id, desc;

        public IdDesc(String id, String desc) {
            this.id = id;
            this.desc = desc;
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    public void testSimpleExplicit() throws Exception
    {
        ObjectMapper mapper = mapperForCsv();
        CsvSchema schema = CsvSchema.builder()
            .addColumn("firstName")
            .addColumn("lastName")
            .addColumn("gender")
            .addColumn("userImage")
            .addColumn("verified")
            .build();

        // from base, default order differs:
        // @JsonPropertyOrder({"firstName", "lastName", "gender" ,"verified", "userImage"})
        
        FiveMinuteUser user = new FiveMinuteUser("Silu", "Seppala", false, Gender.MALE,
                new byte[] { 1, 2, 3, 4, 5});
        String csv = mapper.writer(schema).writeValueAsString(user);
        assertEquals("Silu,Seppala,MALE,AQIDBAU=,false\n", csv);
    }

    public void testSimpleWithAutoSchema() throws Exception
    {
        _testSimpleWithAutoSchema(false);
        _testSimpleWithAutoSchema(true);
    }

    public void testWriteHeaders() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = mapper.schemaFor(FiveMinuteUser.class).withHeader();
        FiveMinuteUser user = new FiveMinuteUser("Barbie", "Benton", false, Gender.FEMALE, null);
        String result = mapper.writer(schema).writeValueAsString(user);        
        assertEquals("firstName,lastName,gender,verified,userImage\n"
                +"Barbie,Benton,FEMALE,false,\n", result);
        
    }

    /**
     * Test that verifies that if a header line is needed, configured schema
     * MUST contain at least one column
     */
    public void testFailedWriteHeaders() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = CsvSchema.builder().setUseHeader(true).build();
        FiveMinuteUser user = new FiveMinuteUser("Barbie", "Benton", false, Gender.FEMALE, null);
        try {
            mapper.writer(schema).writeValueAsString(user);        
            fail("Should fail without columns");
        } catch (JsonGenerationException e) {
            verifyException(e, "contains no column names");
        }
    }

    public void testExplicitWithDouble() throws Exception
    {
        ObjectMapper mapper = mapperForCsv();
        CsvSchema schema = CsvSchema.builder()
            .addColumn("id")
            .addColumn("amount")
            .build();

        String result = mapper.writer(schema).writeValueAsString(new Entry("abc", 1.25));
        assertEquals("abc,1.25\n", result);
    }

    public void testExplicitWithFloat() throws Exception
    {
        ObjectMapper mapper = mapperForCsv();
        CsvSchema schema = CsvSchema.builder()
                .addColumn("id")
                .addColumn("amount")
                .build();

        float amount = 1.89f;
        //this value loses precision when converted
        assertFalse(Double.toString((double)amount).equals("1.89"));
        String result = mapper.writer(schema).writeValueAsString(new Entry2("abc", amount));
        assertEquals("abc,1.89\n", result);
    }

    public void testExplicitWithQuoted() throws Exception
    {
        ObjectMapper mapper = mapperForCsv();
        CsvSchema schema = CsvSchema.builder()
            .addColumn("id")
            .addColumn("desc")
            .build();
        
        String result = mapper.writer(schema).writeValueAsString(new IdDesc("id", "Some \"stuff\""));
        // MUST use doubling for quotes!
        assertEquals("id,\"Some \"\"stuff\"\"\"\n", result);
    }

    // [Issue#14]: String values that cross buffer boundary won't be quoted properly
    public void testLongerWithQuotes() throws Exception
    {
        ObjectMapper mapper = mapperForCsv();
        CsvSchema schema = CsvSchema.builder()
            .addColumn("id")
            .addColumn("desc")
            .build();

        String base = "Longer sequence with bunch of words to test quoting with needs to be at least one line "
                +"long to allow for appropriate indexes and boundary crossing conditions as well";
        
        StringBuilder sb = new StringBuilder();
        do {
            for (String word : base.split("\\s")) {
                sb.append(' ');
                sb.append('"');
                sb.append(word);
                sb.append('"');
            }
        } while (sb.length() < 1050);
        final String inputDesc = sb.toString();
        String expOutputDesc = inputDesc.replace("\"", "\"\"");
        String expOutput = "id,\""+expOutputDesc+"\"";
        String result = mapper.writer(schema).writeValueAsString(new IdDesc("id", inputDesc)).trim();
        assertEquals(expOutput, result);
    }

    public void testWriteInFile() throws Exception
    {
        ObjectMapper mapper = mapperForCsv();
        CsvSchema schema = CsvSchema.builder()
                .addColumn("firstName")
                .addColumn("lastName")
                .build();

        ObjectNode node = mapper.createObjectNode()
                .put("firstName", "David")
                .put("lastName", "Douillet");

        File file = File.createTempFile("file", ".csv");
        try {
            mapper.writer(schema.withHeader()).writeValue(file, node);
        } finally {
            file.delete();
        }
    }

    // [dataformat-csv#53]
    public void testCustomNullValue() throws Exception
    {
        ObjectMapper mapper = mapperForCsv();
        CsvSchema schema = CsvSchema.builder()
                .setNullValue("n/a")
                .addColumn("id")
                .addColumn("desc")
                .build();
        
        String result = mapper.writer(schema).writeValueAsString(new IdDesc("id", null));
        // MUST use doubling for quotes!
        assertEquals("id,n/a\n", result);
    }

    public void testForcedQuoting60() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        mapper.enable(CsvGenerator.Feature.ALWAYS_QUOTE_STRINGS);
        CsvSchema schema = CsvSchema.builder()
            .addColumn("id")
            .addColumn("amount")
            .build();
        String result = mapper.writer(schema)
                .writeValueAsString(new Entry("abc", 1.25));
        assertEquals("\"abc\",1.25\n", result);
    }
    
    /*
    /**********************************************************************
    /* Secondary test methods
    /**********************************************************************
     */

    private void _testSimpleWithAutoSchema(boolean wrapAsArray) throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = mapper.schemaFor(FiveMinuteUser.class);
        FiveMinuteUser user = new FiveMinuteUser("Veltto", "Virtanen", true, Gender.MALE,
                new byte[] { 3, 1 });
        String result;
        // having virtual root-level array should make no difference:
        if (wrapAsArray) {
            result = mapper.writer(schema).writeValueAsString(new FiveMinuteUser[] { user });        
        } else {
            result = mapper.writer(schema).writeValueAsString(user);        
        }
        assertEquals("Veltto,Virtanen,MALE,true,AwE=\n", result);
    }
}
