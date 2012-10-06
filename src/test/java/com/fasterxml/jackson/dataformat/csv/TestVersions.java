package com.fasterxml.jackson.dataformat.csv;

import java.io.*;

import com.fasterxml.jackson.core.*;

public class TestVersions extends ModuleTestBase
{
    // Not a good to do this, but has to do, for now...
    private final static int MAJOR_VERSION = 2;
    private final static int MINOR_VERSION = 1;

    private final static String GROUP_ID = "com.fasterxml.jackson.dataformat";
    private final static String ARTIFACT_ID = "jackson-dataformat-csv";
    
    public void testMapperVersions() throws IOException
    {
        CsvFactory f = new CsvFactory();
        assertVersion(f);
        CsvParser jp = (CsvParser) f.createJsonParser("abc");
        assertVersion(jp);
        CsvGenerator jgen = f.createJsonGenerator(new ByteArrayOutputStream());
        assertVersion(jgen);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */
    
    private void assertVersion(Versioned vers)
    {
        final Version v = vers.version();
        assertFalse("Should find version information (got "+v+")", v.isUknownVersion());
        assertEquals(MAJOR_VERSION, v.getMajorVersion());
        assertEquals(MINOR_VERSION, v.getMinorVersion());
        assertEquals(GROUP_ID, v.getGroupId());
        assertEquals(ARTIFACT_ID, v.getArtifactId());
    }
}

