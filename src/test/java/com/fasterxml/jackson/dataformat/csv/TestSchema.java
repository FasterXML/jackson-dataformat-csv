package com.fasterxml.jackson.dataformat.csv;

public class TestSchema extends ModuleTestBase
{
    public void testSimpleWithAutoSchema() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = mapper.schemaFor(FiveMinuteUser.class);
        assertEquals("[\"firstName\",\"lastName\",\"gender\",\"verified\",\"userImage\"]", schema.getColumnDesc());
    }
}
