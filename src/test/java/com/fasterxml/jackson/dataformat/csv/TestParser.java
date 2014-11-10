package com.fasterxml.jackson.dataformat.csv;

import java.io.ByteArrayOutputStream;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.*;

import static org.junit.Assert.assertArrayEquals;

public class TestParser extends ModuleTestBase
{
    @JsonPropertyOrder({ "x", "y", "z" })
    public static class Point {
        public int x;
        public Integer y;
        public Integer z = 8;
    }    

    final static CsvSchema SIMPLE_SCHEMA = CsvSchema.builder()
            .addColumn("firstName")
            .addColumn("lastName")
            .addColumn("gender")
            .addColumn("userImage")
            .addColumn("verified")
            .build();

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */    

    public void testSimpleExplicit() throws Exception
    {
        ObjectMapper mapper = mapperForCsv();
        ObjectReader r = mapper.reader(SIMPLE_SCHEMA);
        _testSimpleExplicit(r, false);
        _testSimpleExplicit(r, true);
    }

    private void _testSimpleExplicit(ObjectReader r, boolean useBytes) throws Exception
    {
        r = r.withType(FiveMinuteUser.class);
        FiveMinuteUser user;
        final String INPUT = "Bob,Robertson,MALE,AQIDBAU=,false\n";
        if (useBytes) {
            user = r.readValue(INPUT);
        } else {
            user = r.readValue(INPUT.getBytes("UTF-8"));
        }
        assertEquals("Bob", user.firstName);
        assertEquals("Robertson", user.lastName);
        assertEquals(FiveMinuteUser.Gender.MALE, user.getGender());
        assertFalse(user.isVerified());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5}, user.getUserImage());
    }

    public void testSimpleExplicitWithBOM() throws Exception
    {
        ObjectMapper mapper = mapperForCsv();
        ObjectReader r = mapper.reader(SIMPLE_SCHEMA);
        r = r.withType(FiveMinuteUser.class);
        FiveMinuteUser user;

        ByteArrayOutputStream b = new ByteArrayOutputStream();

        // first, UTF-8 BOM:
        b.write(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF });
        b.write("Bob,Robertson,MALE,AQIDBAU=,false\n".getBytes("UTF-8"));
        b.close();

        user = r.readValue(b.toByteArray());
        String fn = user.firstName;

        if (!fn.equals("Bob")) {
            fail("Expected 'Bob' (3), got '"+fn+"' ("+fn.length()+")");
        }
        assertEquals("Robertson", user.lastName);
        assertEquals(FiveMinuteUser.Gender.MALE, user.getGender());
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
        assertEquals(FiveMinuteUser.Gender.MALE, user.getGender());
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
        it.close();
    }

    // Test for [Issue#10]
    public void testMapsWithLinefeeds() throws Exception {
        _testMapsWithLinefeeds(false);
        _testMapsWithLinefeeds(true);
    }

    private void _testMapsWithLinefeeds(boolean useBytes) throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        String CSV = "A,B,C\n"
                +"data11,data12\n"
                +"data21,data22,data23\r\n"
               +"data31,\"data32 data32\ndata32 data32\",data33\n"
                +"data41,\"data42 data42\r\ndata42\",data43\n";
       
        CsvSchema cs = CsvSchema.emptySchema().withHeader();
        ObjectReader or = mapper.reader(HashMap.class).with(cs);
        
        MappingIterator<Map<String,String>> mi;

        if (useBytes) {
            mi = or.readValues(CSV.getBytes("UTF-8"));
        } else {
            mi = or.readValues(CSV);
        }

        assertTrue(mi.hasNext());
        Map<String,String> map = mi.nextValue();
        assertNotNull(map);
        assertEquals("data11", map.get("A"));
        assertEquals("data12", map.get("B"));
        assertEquals(2, map.size());

        assertTrue(mi.hasNext());
        map = mi.nextValue();
        assertNotNull(map);
        assertEquals(3, map.size());

        // then entries with linefeeds
        assertTrue(mi.hasNext());
        map = mi.nextValue();
        assertNotNull(map);
        assertEquals(3, map.size());
        assertEquals("data31", map.get("A"));
        assertEquals("data32 data32\ndata32 data32", map.get("B"));
        assertEquals("data33", map.get("C"));

        assertTrue(mi.hasNext());
        map = mi.nextValue();
        assertNotNull(map);
        assertEquals(3, map.size());
        assertEquals("data41", map.get("A"));
        assertEquals("data42 data42\r\ndata42", map.get("B"));
        assertEquals("data43", map.get("C"));

        assertFalse(mi.hasNext());
        mi.close();
    }

    // [Issue#12]
    public void testEmptyHandlingForInteger() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = mapper.schemaFor(Point.class).withoutHeader();

        // First: empty value, to be considered as null
        Point result = mapper.reader(Point.class).with(schema).readValue(",,\n");
        assertEquals(0, result.x);
        assertNull(result.y);
        assertNull(result.z);
    }

    public void testStringNullHandlingForInteger() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = mapper.schemaFor(Point.class).withoutHeader();

        // First: empty value, to be considered as null
        Point result = mapper.reader(Point.class).with(schema).readValue("null,null,null\n");
        assertEquals(0, result.x);
        assertNull(result.y);
        assertNull(result.z);
    }

    // [Issue#41]
    public void testIncorrectDups41() throws Exception
    {
        final String INPUT = "\"foo\",\"bar\",\"foo\"";
        CsvSchema schema = CsvSchema.builder().addColumn("Col1").addColumn("Col2")
                .addColumn("Col3").build();

        MappingIterator<Object> iter = new CsvMapper().reader(Object.class)
                .with(schema).readValues(INPUT);

        Map<?,?> m  = (Map<?,?>) iter.next();
        assertFalse(iter.hasNextValue());
        iter.close();

        if (m.size() != 3) {
            fail("Should have 3 entries, but got: "+m);
        }
        assertEquals("foo", m.get("Col1"));
        assertEquals("bar", m.get("Col2"));
        assertEquals("foo", m.get("Col3"));
    }
}
