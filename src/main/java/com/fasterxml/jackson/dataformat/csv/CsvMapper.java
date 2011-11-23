package com.fasterxml.jackson.dataformat.csv;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.type.TypeReference;

import com.fasterxml.jackson.dataformat.csv.impl.LRUMap;

/**
 * Specialized {@link ObjectMapper}, with extended functionality to
 * produce {@link CsvSchema} instances out of POJOs.
 */
public class CsvMapper extends ObjectMapper
{
    /**
     * Simple caching for schema instances, given that they are relatively expensive
     * to construct; this one is for "loose" (non-typed) schemas
     */
    protected final LRUMap<JavaType,CsvSchema> _untypedSchemas = new LRUMap<JavaType,CsvSchema>(8,32);

    /**
     * Simple caching for schema instances, given that they are relatively expensive
     * to construct; this one is for typed schemas
     */
    protected final LRUMap<JavaType,CsvSchema> _typedSchemas = new LRUMap<JavaType,CsvSchema>(8,32);
    
    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public CsvMapper()
    {
        this(new CsvFactory());
    }

    public CsvMapper(CsvFactory f)
    {
        super(f);
    }

    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */
    
    public CsvMapper configure(CsvGenerator.Feature f, boolean state) {
        ((CsvFactory)_jsonFactory).configure(f, state);
        return this;
    }

    public CsvMapper configure(CsvParser.Feature f, boolean state) {
        ((CsvFactory)_jsonFactory).configure(f, state);
        return this;
    }

    /*
    /**********************************************************************
    /* ObjectReader/ObjectWriter factories
    /**********************************************************************
     */
    
    /*
    /**********************************************************************
    /* CsvSchema construction
    /**********************************************************************
     */

    /**
     * Method that can be used to determine a CSV schema to use for given
     * POJO type, using default serialization settings including ordering.
     * Definition will not be strictly typed (that is, all columns are
     * just defined to be exposed as String tokens).
     */
    public CsvSchema schemaFor(JavaType pojoType)
    {
        return _schemaFor(pojoType, _untypedSchemas, false);
    }

    public final CsvSchema schemaFor(Class<?> pojoType) {
        return _schemaFor(constructType(pojoType), _untypedSchemas, false);
    }

    public final CsvSchema schemaFor(TypeReference<?> pojoTypeRef) {
        return _schemaFor(constructType(pojoTypeRef.getType()), _untypedSchemas, false);
    }

    /**
     * Method that can be used to determine a CSV schema to use for given
     * POJO type, using default serialization settings including ordering.
     * Definition will not be strictly typed (that is, all columns are
     * just defined to be exposed as String tokens).
     */
    public CsvSchema typedSchemaFor(JavaType pojoType)
    {
        return _schemaFor(pojoType, _typedSchemas, true);
    }

    public final CsvSchema typedSchemaFor(Class<?> pojoType) {
        return _schemaFor(constructType(pojoType), _typedSchemas, true);
    }

    public final CsvSchema typedSchemaFor(TypeReference<?> pojoTypeRef) {
        return _schemaFor(constructType(pojoTypeRef.getType()), _typedSchemas, true);
    }

    protected CsvSchema _schemaFor(JavaType pojoType, LRUMap<JavaType,CsvSchema> schemas, boolean typed)
    {
        synchronized (schemas) {
            CsvSchema s = schemas.get(pojoType);
            if (s != null) {
                return s;
            }
        }
        BeanDescription beanDesc = getSerializationConfig().introspect(pojoType);
        CsvSchema.Builder builder = CsvSchema.builder();
        for (BeanPropertyDefinition prop : beanDesc.findProperties()) {
            // ignore setter-only properties:
            if (prop.couldSerialize()) {
                if (typed) {
                    builder.addColumn(prop.getName(), _determineType(prop.getAccessor().getRawType()));
                } else {
                    builder.addColumn(prop.getName());
                }
            }
        }
        CsvSchema result = builder.build();
        synchronized (schemas) {
            schemas.put(pojoType, result);
        }
        return result;
    }
    
    @Override
    public ObjectReader reader(FormatSchema schema)
    {
        if ((schema != null) && !(schema instanceof CsvSchema)) {
            throw new IllegalArgumentException("Schema to set must be of type CsvSchema");
        }
        return super.reader(schema);
    }

    @Override
    public ObjectWriter writer(FormatSchema schema)
    {
        if ((schema != null) && !(schema instanceof CsvSchema)) {
            throw new IllegalArgumentException("Schema to set must be of type CsvSchema");
        }
        return super.writer(schema);
    }

    // deprecated:
    public ObjectReader schemaBasedReader(FormatSchema schema) { return reader(schema); }
    // deprecated:
    public ObjectWriter schemaBasedWriter(FormatSchema schema) { return writer(schema); }

    /*
    /**********************************************************************
    /* Internal methods
    /**********************************************************************
     */
    
    // should not be null since couldSerialize() returned true, so:
    protected CsvSchema.ColumnType _determineType(Class<?> propType)
    {
        // First let's check certain cases that ought to be just presented as Strings...
        if (propType == String.class
                || propType == Character.TYPE
                || propType == Character.class) {
            return CsvSchema.ColumnType.STRING;
        }
        // all primitive types are good, since "numeric" allows boolean
        if (propType.isPrimitive() || Number.class.isAssignableFrom(propType)) {
            return CsvSchema.ColumnType.NUMBER;
        }
        // but in general we will just do what we can:
        return CsvSchema.ColumnType.NUMBER_OR_STRING;
    }

}
