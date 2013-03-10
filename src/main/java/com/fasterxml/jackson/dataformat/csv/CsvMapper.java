package com.fasterxml.jackson.dataformat.csv;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.dataformat.csv.impl.LRUMap;

/**
 * Specialized {@link ObjectMapper}, with extended functionality to
 * produce {@link CsvSchema} instances out of POJOs.
 */
public class CsvMapper extends ObjectMapper
{
    private static final long serialVersionUID = 898121211926121724L;

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
        return state ? enable(f) : disable(f);
    }

    public CsvMapper configure(CsvParser.Feature f, boolean state) {
        return state ? enable(f) : disable(f);
    }

    public CsvMapper enable(CsvGenerator.Feature f) {
        ((CsvFactory)_jsonFactory).enable(f);
        return this;
    }

    public CsvMapper enable(CsvParser.Feature f) {
        ((CsvFactory)_jsonFactory).enable(f);
        return this;
    }

    public CsvMapper disable(CsvGenerator.Feature f) {
        ((CsvFactory)_jsonFactory).disable(f);
        return this;
    }

    public CsvMapper disable(CsvParser.Feature f) {
        ((CsvFactory)_jsonFactory).disable(f);
        return this;
    }

    /*
    /**********************************************************************
    /* Additional typed accessors
    /**********************************************************************
     */

    /**
     * Overridden with more specific type, since factory we have
     * is always of type {@link CsvFactory}
     */
    @Override
    public final CsvFactory getFactory() {
        return (CsvFactory) _jsonFactory;
    }
    
    /**
     * Overridden with more specific type, since factory we have
     * is always of type {@link CsvFactory}
     * 
     * @deprecated Since 2.1 use {@link #getFactory()}
     */
    @Deprecated
    @Override
    public final CsvFactory getJsonFactory() {
        return getCsvFactory();
    }

    /**
     * Type-safe accessor for accessing underlying CSV-specific
     * {@link JsonFactory} implementation
     * 
     * @deprecated Since 2.1 use {@link #getFactory()}, it is co-variant
     */
    @Deprecated
    public final CsvFactory getCsvFactory() {
        return (CsvFactory) _jsonFactory;
    }
    
    /*
    /**********************************************************************
    /* CsvSchema construction; overrides, new methods
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
     * Definition WILL be strictly typed: that is, code will try to 
     * determine type limitations which may make parsing more efficient
     * (especially for numeric types like java.lang.Integer).
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

    /**
     * Convenience method which is functionally equivalent to:
     *<pre>
     *  reader(pojoType).withSchema(schemaFor(pojoType));
     *</pre>
     * that is, constructs a {@link ObjectReader} which both binds to
     * specified type and uses "loose" {@link CsvSchema} introspected from
     * specified type (one without strict inferred typing).
     *<p>
     * @param pojoType Type used both for data-binding (result type) and for
     *   schema introspection. NOTE: must NOT be an array or Collection type, since
     *   these only make sense for data-binding (like arrays of objects to bind),
     *   but not for schema construction (no CSV types can be mapped to arrays
     *   or Collections)
     */
    public ObjectReader readerWithSchemaFor(Class<?> pojoType)
    {
        JavaType type = constructType(pojoType);
        /* sanity check: not useful for structured types, since
         * schema type will need to differ from data-bind type
         */
        if (type.isArrayType() || type.isCollectionLikeType()) {
            throw new IllegalArgumentException("Type can NOT be a Collection or array type");
        }
        return reader(type).with(schemaFor(type));
    }

    /**
     * Convenience method which is functionally equivalent to:
     *<pre>
     *  reader(pojoType).withSchema(typedSchemaFor(pojoType));
     *</pre>
     * that is, constructs a {@link ObjectReader} which both binds to
     * specified type and uses "strict" {@link CsvSchema} introspected from
     * specified type (one where typing is inferred).
     */
    public ObjectReader readerWithTypedSchemaFor(Class<?> pojoType)
    {
        JavaType type = constructType(pojoType);
        /* sanity check: not useful for structured types, since
         * schema type will need to differ from data-bind type
         */
        if (type.isArrayType() || type.isCollectionLikeType()) {
            throw new IllegalArgumentException("Type can NOT be a Collection or array type");
        }
        return reader(type).with(typedSchemaFor(type));
    }

    /*
    /**********************************************************************
    /* Internal methods
    /**********************************************************************
     */

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
