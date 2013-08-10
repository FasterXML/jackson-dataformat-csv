package com.fasterxml.jackson.dataformat.csv;

public class TestGeneratorWithCustomSeparators extends ModuleTestBase
{
    // #17
    public void testOtherSeparator() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = mapper.schemaFor(FiveMinuteUser.class).withoutHeader().withColumnSeparator(';');
        FiveMinuteUser user = new FiveMinuteUser("Barbie", "Benton", false, FiveMinuteUser.Gender.FEMALE, null);
        String result = mapper.writer(schema).writeValueAsString(user);
        assertEquals("Barbie;Benton;FEMALE;false;\n", result);
    }
}
