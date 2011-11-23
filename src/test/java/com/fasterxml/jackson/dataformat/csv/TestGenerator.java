package com.fasterxml.jackson.dataformat.csv;

import org.codehaus.jackson.map.ObjectMapper;

import com.fasterxml.jackson.dataformat.csv.ModuleTestBase.FiveMinuteUser.Gender;

public class TestGenerator extends ModuleTestBase
{
    public void testSimple() throws Exception
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
}
