package com.fasterxml.jackson.dataformat.csv;

import java.util.*;

import org.codehaus.jackson.map.MappingIterator;
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
        // NOTE: order different from above test (as per POJO def!)
        FiveMinuteUser user = mapper.reader(schema).withType(FiveMinuteUser.class).readValue("Joe,Josephson,MALE,true,AwE=\n");
        assertEquals("Joe", user.firstName);
        assertEquals("Josephson", user.lastName);
        assertEquals(Gender.MALE, user.getGender());
        assertTrue(user.isVerified());
        assertArrayEquals(new byte[] { 3, 1 }, user.getUserImage());
    }

    /**
     * Test to verify that we can mix "untyped" access as Maps
     * with schema information...
     */
    public void testSimpleAsMaps() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = mapper.schemaFor(FiveMinuteUser.class);
        MappingIterator<Map<?,?>> it = mapper.reader(schema).withType(Map.class).readValues(
                "Joe,Smith,MALE,false,"
                );
        assertTrue(it.hasNext());
        Map<?,?> result = it.nextValue();
        assertEquals(5, result.size());
        assertEquals("Joe", result.get("firstName"));
        assertEquals("Smith", result.get("lastName"));
        assertEquals("MALE", result.get("gender"));
        assertEquals("false", result.get("verified"));
        assertEquals("", result.get("userImage"));

        assertFalse(it.hasNextValue());
    }
}
