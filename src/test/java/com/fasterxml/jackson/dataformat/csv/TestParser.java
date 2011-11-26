package com.fasterxml.jackson.dataformat.csv;

import org.codehaus.jackson.map.ObjectMapper;

import static org.junit.Assert.assertArrayEquals;

import com.fasterxml.jackson.dataformat.csv.ModuleTestBase.FiveMinuteUser.Gender;

public class TestParser extends ModuleTestBase
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

        FiveMinuteUser user = mapper.reader(schema).withType(FiveMinuteUser.class).readValue("Bob,Robertson,MALE,AQIDBAU=,false\n");
        assertEquals("Bob", user.firstName);
        assertEquals("Robertson", user.lastName);
        assertEquals(Gender.MALE, user.getGender());
        assertFalse(user.isVerified());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5}, user.getUserImage());
    }

    public void testSimpleWithAutoSchema() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = mapper.schemaFor(FiveMinuteUser.class);
        FiveMinuteUser user = mapper.reader(schema).withType(FiveMinuteUser.class).readValue("Joe,Josephson,MALE,AwE=,true\n");
        assertEquals("Joe", user.firstName);
        assertEquals("Josephson", user.lastName);
        assertEquals(Gender.MALE, user.getGender());
        assertTrue(user.isVerified());
        assertArrayEquals(new byte[] { 3, 1 }, user.getUserImage());
    }
}
