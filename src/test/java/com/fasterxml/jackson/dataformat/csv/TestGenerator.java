package com.fasterxml.jackson.dataformat.csv;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonGenerationException;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.dataformat.csv.ModuleTestBase.FiveMinuteUser.Gender;

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
    /* Unit tests
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

        FiveMinuteUser user = new FiveMinuteUser("Silu", "Seppala", false, Gender.MALE,
                new byte[] { 1, 2, 3, 4, 5});
        String result = mapper.writer(schema).writeValueAsString(user);        
        assertEquals("Silu,Seppala,MALE,AQIDBAU=,false\n", result);
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

    public void testExplicitWithQupted() throws Exception
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
