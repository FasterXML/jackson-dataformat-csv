package com.fasterxml.jackson.dataformat.csv;

import java.io.*;

import com.fasterxml.jackson.core.*;

public class TestVersions extends ModuleTestBase
{
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
        assertEquals(PackageVersion.VERSION, vers.version());
    }
}

