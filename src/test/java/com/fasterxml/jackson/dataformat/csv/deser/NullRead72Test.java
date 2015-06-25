package com.fasterxml.jackson.dataformat.csv.deser;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.csv.*;

public class NullRead72Test extends ModuleTestBase
{
    final CsvMapper MAPPER = mapperForCsv();

    // For [dataformat-csv#72]: recognize "null value" for reading too
    public void testReadNullValue() throws Exception
    {
        CsvSchema schema = CsvSchema.builder()
                .setNullValue("n/a")
                .addColumn("id")
                .addColumn("desc")
                .build();

        // start by writing, first
        String csv = MAPPER.writer(schema).writeValueAsString(new IdDesc("id", null));
        // MUST use doubling for quotes!
        assertEquals("id,n/a\n", csv);

        // but read back
        
        ObjectReader r = MAPPER.readerFor(IdDesc.class)
                .with(schema);

        IdDesc result = r.readValue(csv);
        assertNotNull(result);
        assertEquals("id", result.id);
        assertNull(result.desc);

        // also try the other combination
        result = r.readValue("n/a,Whatevs\n");
        assertNotNull(result);
        assertNull(result.id);
        assertEquals("Whatevs", result.desc);
    }
}
