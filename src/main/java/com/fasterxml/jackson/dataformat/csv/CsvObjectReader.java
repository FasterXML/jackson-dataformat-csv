package com.fasterxml.jackson.dataformat.csv;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.DataFormatReaders;

/**
 * @since 2.5
 */
public class CsvObjectReader
    extends ObjectReader
{
    private static final long serialVersionUID = 1L;

    public CsvObjectReader(ObjectMapper mapper, DeserializationConfig config) {
        super(mapper, config);
    }

    protected CsvObjectReader(ObjectMapper mapper, DeserializationConfig config,
            JavaType valueType, Object valueToUpdate,
            FormatSchema schema, InjectableValues injectableValues) {
        super(mapper, config, valueType, valueToUpdate, schema, injectableValues);
    }

    protected CsvObjectReader(ObjectReader base, DeserializationConfig config,
            JavaType valueType, JsonDeserializer<Object> rootDeser, Object valueToUpdate,
            FormatSchema schema, InjectableValues injectableValues,
            DataFormatReaders dataFormatReaders) {
        super(base, config, valueType, rootDeser, valueToUpdate,
                schema, injectableValues, dataFormatReaders);
    }

    protected CsvObjectReader(ObjectReader base, DeserializationConfig config) {
        super(base, config);
    }

    protected CsvObjectReader(ObjectReader base, JsonFactory f) {
        super(base, f);
    }
    
    @Override
    protected ObjectReader _new(ObjectReader base, JsonFactory f) {
        return new CsvObjectReader(base, f);
    }

    @Override
    protected ObjectReader _new(ObjectReader base, DeserializationConfig config) {
        return new CsvObjectReader(base, config);
    }

    @Override
    protected ObjectReader _new(ObjectReader base, DeserializationConfig config,
            JavaType valueType, JsonDeserializer<Object> rootDeser, Object valueToUpdate,
            FormatSchema schema, InjectableValues injectableValues,
            DataFormatReaders dataFormatReaders) {
        return new CsvObjectReader(base, config, valueType, rootDeser,  valueToUpdate,
                 schema,  injectableValues, dataFormatReaders);
    }

    /*
    @Override
    protected <T> MappingIterator<T> _newIterator(JavaType valueType,
            JsonParser parser, DeserializationContext ctxt,
            JsonDeserializer<?> deser, boolean parserManaged, Object valueToUpdate)
    {
            return new MappingIterator<T>(valueType, parser, ctxt,
                    deser, parserManaged, valueToUpdate);
    }
    */
}
