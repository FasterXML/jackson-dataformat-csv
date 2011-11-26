package com.fasterxml.jackson.dataformat.csv;

import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.ObjectMapper;

import com.fasterxml.jackson.dataformat.csv.ModuleTestBase.FiveMinuteUser.Gender;

public class TestGenerator extends ModuleTestBase
{
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
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = mapper.schemaFor(FiveMinuteUser.class);
        FiveMinuteUser user = new FiveMinuteUser("Veltto", "Virtanen", true, Gender.MALE,
                new byte[] { 3, 1 });
        String result = mapper.writer(schema).writeValueAsString(user);        
        assertEquals("Veltto,Virtanen,MALE,true,AwE=\n", result);
    }
}
