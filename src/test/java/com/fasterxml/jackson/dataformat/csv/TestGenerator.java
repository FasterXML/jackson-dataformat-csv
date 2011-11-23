package com.fasterxml.jackson.dataformat.csv;

import org.codehaus.jackson.map.ObjectMapper;

import com.fasterxml.jackson.dataformat.csv.ModuleTestBase.FiveMinuteUser.Gender;

public class TestGenerator extends ModuleTestBase
{
    public void testSimple() throws Exception
    {
        CsvFactory f = new CsvFactory();
        f.setSchema(new CsvSchema.Builder()
            .addColumn("firstName")
            .addColumn("lastName")
            .addColumn("gender")
            .addColumn("userImage")
            .addColumn("verified")
            .build());
            
        ObjectMapper mapper = new ObjectMapper(f);
        FiveMinuteUser user = new FiveMinuteUser("Silu", "Seppala", false, Gender.MALE,
                new byte[] { 1, 2, 3, 4, 5});
        String result = mapper.writeValueAsString(user);        
        assertEquals("Silu,Seppala,MALE,AQIDBAU=,false\n", result);
    }
}
