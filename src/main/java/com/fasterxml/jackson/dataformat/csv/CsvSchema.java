package com.fasterxml.jackson.dataformat.csv;

import java.util.*;

import org.codehaus.jackson.FormatSchema;

/**
 * Simple {@link FormatSchema} sub-type which supports simple
 * (non-nested) tabular data format, where data consists of rows
 * of homogenous data, where typing is positional.
 * This schema can be used as basetype for many simple tabular
 * data format; for example CSV schema can be based on this.
 * Names for columns are optional; and while types are not optional
 * it is possible to use basic {@link java.lang.Object} as placeholder
 * if more specific typ is not available.
 *
 * @since 1.9
 */
public class CsvSchema 
    implements FormatSchema
{
    /**
     * Enumeration that defines optional type indicators that can be passed
     * with schema. If used type is used to determine type of {@link JsonToken}
     * that column values are exposed as.
     */
    public enum ColumnType
    {
        /**
         * Default type if not explicitly defined; value will
         * be presented as <code>VALUE_STRING</code> by parser,
         * that is, no type-inference is performed
         */
        STRING,

        /**
         * Value is considered to be a String, except that tokens
         * "null", "true" and "false" are recognized as matching
         * tokens and reported as such.
         */
        STRING_OR_LITERAL,
        
        /**
         * Value should be a number, but literals "null", "true" and "false"
         * are also understood, and an empty String is considered null.
         * Other non-numeric Strings consider parsing exception.
         */
        NUMBER,

        /**
         * Value is taken to be a number (if it matches valid JSON number
         * formatting rules), literal (null, true or false) or String,
         * depending on best match.
         */
        NUMBER_OR_STRING
        
        ;
    }

    public static class Column
    {
        private final String _name;
        private final ColumnType _type;

        public Column(String name) {
            this(name, ColumnType.STRING);
        }

        public Column(String name, ColumnType type)
        {
            _name = name;
            _type = type;
        }

        public String getName() { return _name; }
        public ColumnType getType() { return _type; }
    }
    
    /**
     * Class used for building {@link CsvSchema} instances.
     */
    public static class Builder
    {
        protected final ArrayList<Column> _columns = new ArrayList<Column>();
        
        public Builder() { }

        public Builder addColumn(String name) {
            return addColumn(new Column(name));
        }
        public Builder addColumn(String name, ColumnType type) {
            return addColumn(new Column(name, type));
        }
        
        public Builder addColumn(Column c) {
            _columns.add(c);
            return this;
        }

        public CsvSchema build()
        {
            Column[] cols = _columns.toArray(new Column[_columns.size()]);
            return new CsvSchema(cols);
        }
    }

    /*
    /**********************************************************************
    /* Configuration, construction
    /**********************************************************************
     */
 
    protected final static Column[] NO_COLUMNS = new Column[0];
    
    /**
     * Column definitions, needed for optional header and/or mapping
     * of field names to column positions.
     */
    protected final Column[] _columns;

    protected final Map<String,Column> _columnsByName;
    
    public CsvSchema(Column[] columns)
    {
        if (columns == null) {
            columns = NO_COLUMNS;
        }
        _columns = columns;
        // and then we may need to create a mapping
        if (_columns.length == 0) {
            _columnsByName = Collections.emptyMap();
        } else {
            _columnsByName = new HashMap<String,Column>(4 + _columns.length);
            for (Column c : _columns) {
                _columnsByName.put(c.getName(), c);
            }
        }
    }
    
    /*
    /**********************************************************************
    /* Public API, FormatSchema
    /**********************************************************************
     */

    @Override
    public String getSchemaType() {
        return "CSV";
    }

    /*
    /**********************************************************************
    /* Public API, extended
    /**********************************************************************
     */

    public int size() { return _columns.length; }
    
    public Column column(int index) {
        return _columns[index];
    }

    public Column column(String name) {
        return _columnsByName.get(name);
    }
}
