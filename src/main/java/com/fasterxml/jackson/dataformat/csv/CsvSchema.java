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
    implements FormatSchema,
        Iterable<CsvSchema.Column>
{
    /*
    /**********************************************************************
    /* Constants
    /**********************************************************************
     */
 
    protected final static Column[] NO_COLUMNS = new Column[0];

    public final static char DEFAULT_COLUMN_SEPARATOR = ',';
    
    public final static char DEFAULT_QUOTE_CHAR = '"';

    /**
     * By default, no escape character is used -- this is denoted by
     * int value that does not map to a valid character
     */
    public final static int DEFAULT_ESCAPE_CHAR = -1;
    
    public final static char[] DEFAULT_LINEFEED = "\n".toCharArray();

    /**
     * By default we do NOT expect the first line to be header.
     */
    public final static boolean DEFAULT_USE_HEADER = false;
    
    /*
    /**********************************************************************
    /* Helper classes
    /**********************************************************************
     */
    
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
        private final int _index;
        private final ColumnType _type;

        public Column(int index, String name) {
            this(index, name, ColumnType.STRING);
        }

        public Column(int index, String name, ColumnType type)
        {
            _index = index;
            _name = name;
            _type = type;
        }

        public int getIndex() { return _index; }
        public String getName() { return _name; }
        public ColumnType getType() { return _type; }
    }
    
    /**
     * Class used for building {@link CsvSchema} instances.
     */
    public static class Builder
    {
        protected final ArrayList<Column> _columns = new ArrayList<Column>();

        protected boolean _useHeader = DEFAULT_USE_HEADER;

        protected char _columnSeparator = DEFAULT_COLUMN_SEPARATOR;

        protected char _quoteChar = DEFAULT_QUOTE_CHAR;

        // note: need to use int to allow -1 for 'none'
        protected int _escapeChar = DEFAULT_QUOTE_CHAR;
        
        protected char[] _lineSeparator = DEFAULT_LINEFEED;
        
        public Builder() { }

        /**
         * "Copy" constructor which creates builder that has settings of
         * given source schema
         */
        public Builder(CsvSchema src)
        {
            for (Column col : src._columns) {
                _columns.add(col);
            }
            _useHeader = src._useHeader;
            _columnSeparator = src._columnSeparator;
            _quoteChar = src._quoteChar;
            _escapeChar = src._escapeChar;
            _lineSeparator = src._lineSeparator;
        }
        
        public Builder addColumn(String name) {
            int index = _columns.size();
            return addColumn(new Column(index, name));
        }
        public Builder addColumn(String name, ColumnType type) {
            int index = _columns.size();
            return addColumn(new Column(index, name, type));
        }
        
        protected Builder addColumn(Column c) {
            _columns.add(c);
            return this;
        }

        public Builder removeColumns() {
            _columns.clear();
            return this;
        }

        public int getColumnCount() {
            return _columns.size();
        }

        public Iterator<Column> getColumns() {
            return _columns.iterator();
        }
        
        /**
         * Method for specifying whether Schema should indicate that
         * a header line (first row that contains column names) is to be
         * used for reading and writing or not.
         */
        public Builder setUseHeader(boolean b) {
            _useHeader = b;
            return this;
        }
        
        /**
         * Method for specifying character used to separate column
         * values.
         * Default is comma (',').
         */
        public Builder setColumnSeparator(char c) {
            _columnSeparator = c;
            return this;
        }

        /**
         * Method for specifying character used for optional quoting
         * of values.
         * Default is double-quote ('"').
         */
        public Builder setQuoteChar(char c) {
            _quoteChar = c;
            return this;
        }

        /**
         * Method for specifying character used for optional escaping
         * of characters in quoted String values.
         * Default is "not used", meaning that no escaping used.
         */
        public Builder setEscapeChar(char c) {
            _escapeChar = (int) c;
            return this;
        }

        /**
         * Method for specifying that no escape character is to be used
         * with CSV documents this schema defines.
         */
        public Builder disableEscapeChar() {
            _escapeChar = -1;
            return this;
        }
        
        public Builder setLineSeparator(String lf) {
            _lineSeparator = lf.toCharArray();
            return this;
        }

        public Builder setLineSeparator(char lf) {
            _lineSeparator = new char[] { lf };
            return this;
        }
        
        public CsvSchema build()
        {
            Column[] cols = _columns.toArray(new Column[_columns.size()]);
            return new CsvSchema(cols,
                    _useHeader, _columnSeparator, _quoteChar, _escapeChar, _lineSeparator);
        }
    }
    
    /*
    /**********************************************************************
    /* Configuration, construction
    /**********************************************************************
     */
    
    /**
     * Column definitions, needed for optional header and/or mapping
     * of field names to column positions.
     */
    protected final Column[] _columns;
    
    protected final Map<String,Column> _columnsByName;

    protected final boolean _useHeader;

    protected final char _columnSeparator;

    protected final char _quoteChar;
    
    protected final int _escapeChar;
    
    protected final char[] _lineSeparator;
    
    public CsvSchema(Column[] columns,
            boolean useHeader, char columnSeparator, char quoteChar, int escapeChar,
            char[] lineSeparator)
    {
        if (columns == null) {
            columns = NO_COLUMNS;
        }
        _columns = columns;
 
        _useHeader = useHeader;
        _columnSeparator = columnSeparator;
        _quoteChar = quoteChar;
        _escapeChar = escapeChar;
        _lineSeparator = lineSeparator;
        
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

    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Helper method for constructing Builder that can be used to create modified
     * schema.
     */
    public Builder copy() {
        return new Builder(this);
    }

    public static CsvSchema emptySchema() {
        return builder().build();
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
    /* Public API, extended, properties
    /**********************************************************************
     */

    public boolean useHeader() { return _useHeader; }
    public char getColumnSeparator() { return _columnSeparator; }
    public char getQuoteChar() { return _quoteChar; }
    public int getEscapeChar() { return _escapeChar; }
    public char[] getLineSeparator() { return _lineSeparator; }
    
    /*
    /**********************************************************************
    /* Public API, extended; column access
    /**********************************************************************
     */
    
    @Override
    public Iterator<Column> iterator() {
        return Arrays.asList(_columns).iterator();
    }
    
    public int size() { return _columns.length; }
    
    public Column column(int index) {
        return _columns[index];
    }

    public Column column(String name) {
        return _columnsByName.get(name);
    }
    
    /**
     * Method for getting description of column definitions in
     * developer-readable form
     */
    public String getColumnDesc()
    {
        StringBuilder sb = new StringBuilder(100);
        for (Column col : _columns) {
            if (sb.length() == 0) {
                sb.append('[');
            } else {
                sb.append(',');
            }
            sb.append('"');
            sb.append(col.getName());
            sb.append('"');
        }
        sb.append(']');
        return sb.toString();
    }
    
    /*
    /**********************************************************************
    /* Other
    /**********************************************************************
     */

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(150);
        sb.append("[CsvSchema: ")
            .append("columns=")
            .append(getColumnDesc())
            .append(']');
        return sb.toString();
    }
}
