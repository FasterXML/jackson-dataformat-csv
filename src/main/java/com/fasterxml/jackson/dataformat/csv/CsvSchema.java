package com.fasterxml.jackson.dataformat.csv;

import java.util.*;

import com.fasterxml.jackson.core.FormatSchema;

/**
 * Simple {@link FormatSchema} sub-type that defines properties of
 * a CSV document to read or write.
 * Properties supported currently are:
 *<ul>
 * <li>columns (List of ColumnDef) [default: empty List]: Ordered list of columns (which may be empty, see below).
 *   Each column has name (mandatory)  as well as type (optional; if not
 *   defined, defaults to "String").
 *   Note that
 *  </li>
 * <li>useHeader (boolean) [default: false]: whether the first line of physical document defines
 *    column names (true) or not (false): if enabled, parser will take
 *    first-line values to define column names; and generator will output
 *    column names as the first line
 *  </li>
 * <li>quoteChar (char) [default: double-quote ('")]: character used for quoting values
 *   that contain quote characters or linefeeds.
 *  </li>
 * <li>columnSeparator (char) [default: comma (',')]: character used to separate values.
 *     Other commonly used values include tab ('\t') and pipe ('|')
 *  </li>
 * <li>lineSeparator (String) [default: "\n"]: character used to separate data rows.
 *    Only used by generator; parser accepts three standard linefeeds ("\r", "\r\n", "\n").
 *  </li>
 * <li>escapeChar (int) [default: -1 meaning "none"]: character, if any, used to
 *   escape values. Most commonly defined as backslash ('\'). Only used by parser;
 *   generator only uses quoting, including doubling up of quotes to indicate quote char
 *   itself.
 *  </li>
 * <li>skipFirstDataRow (boolean) [default: false]: whether the first data line (either
 *    first line of the document, if useHeader=false, or second, if useHeader=true)
 *    should be completely ignored by parser. Needed to support CSV-like file formats
 *    that include additional non-data content before real data begins (specifically
 *    some database dumps do this)
 *  </li>
 * </ul>
 *<p>
 * Note that schemas without any columns are legal, but if no columns
 * are added, behavior of parser/generator is usually different and
 * content will be exposed as logical Arrays instead of Objects.
 *<p>
 * There are 4 ways to create <code>CsvSchema</code> instances:
 *<ul>
 * <li>Manually build one, using {@link Builder}
 *  </li>
 * <li>Modify existing schema (using <code>withXxx</code> methods
 *    or {@link #rebuild} for creating {@link Builder})
 *  </li>
 * <li>Create schema based on a POJO definition (Class), using
 *    {@link CsvMapper} methods like {@link CsvMapper#schemaFor(java.lang.Class)}.
 *  </li>
 * <li>Request that {@link CsvParser} reads schema from the first line:
 *    enable "useHeader" property for the initial schema, and let parser
 *    read column names from the document itself.
 *  </li>
 *</ul>
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

    public final static boolean DEFAULT_SKIP_FIRST_DATA_ROW = false;

    /*
    /**********************************************************************
    /* Helper classes
    /**********************************************************************
     */
    
    /**
     * Enumeration that defines optional type indicators that can be passed
     * with schema. If used type is used to determine type of
     * {@link com.fasterxml.jackson.core.JsonToken}
     * that column values are exposed as.
     */
    public enum ColumnType
    {
        /**
         * Default type if not explicitly defined; value will
         * be presented as <code>VALUE_STRING</code> by parser,
         * that is, no type-inference is performed, and value is
         * not trimmed.
         */
        STRING,

        /**
         * Value is considered to be a String, except that tokens
         * "null", "true" and "false" are recognized as matching
         * tokens and reported as such;
         * and values are trimmed (leading/trailing white space)
         */
        STRING_OR_LITERAL,
        
        /**
         * Value should be a number, but literals "null", "true" and "false"
         * are also understood, and an empty String is considered null.
         * Values are also trimmed (leading/trailing white space)
         * Other non-numeric Strings will cause parsing exception.
         */
        NUMBER,

        /**
         * Value is taken to be a number (if it matches valid JSON number
         * formatting rules), literal (null, true or false) or String,
         * depending on best match.
         * Values are also trimmed (leading/trailing white space)
         */
        NUMBER_OR_STRING
        
        ;
    }

    /**
     * Representation of info for a single column
     */
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

        public Column withName(String newName) {
            return new Column(_index, newName, _type);
        }
        public Column withType(ColumnType newType) {
            return new Column(_index, _name, newType);
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

        protected boolean _skipFirstDataRow = DEFAULT_SKIP_FIRST_DATA_ROW;
        
        protected char _columnSeparator = DEFAULT_COLUMN_SEPARATOR;

        // note: need to use int to allow -1 for 'none'
        protected int _quoteChar = DEFAULT_QUOTE_CHAR;

        // note: need to use int to allow -1 for 'none'
        protected int _escapeChar = DEFAULT_ESCAPE_CHAR;
        
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
            _skipFirstDataRow = src._skipFirstDataRow;
        }
        
        public Builder addColumn(String name) {
            int index = _columns.size();
            return addColumn(new Column(index, name));
        }
        public Builder addColumn(String name, ColumnType type) {
            int index = _columns.size();
            return addColumn(new Column(index, name, type));
        }
        public Builder addColumn(Column c) {
            _columns.add(c);
            return this;
        }
        public void replaceColumn(int index, Column c) {
            _checkIndex(index);
            _columns.set(index, c);
        }
        public void renameColumn(int index, String newName) {
            _checkIndex(index);
            _columns.set(index, _columns.get(index).withName(newName));
        }
        public void setColumnType(int index, ColumnType type) {
            _checkIndex(index);
            _columns.set(index, _columns.get(index).withType(type));
        }

        public Builder clearColumns() {
            _columns.clear();
            return this;
        }

        public int size() {
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

        public Builder setSkipFirstDataRow(boolean b) {
            _skipFirstDataRow = b;
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
         * @since 2.4
         */
        public Builder disableQuoteChar() {
            _quoteChar = -1;
            return this;
        }
        
        /**
         * Method for specifying character used for optional escaping
         * of characters in quoted String values.
         * Default is "not used", meaning that no escaping used.
         */
        public Builder setEscapeChar(char c) {
            _escapeChar = c;
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
                    _useHeader, _skipFirstDataRow,
                    _columnSeparator, _quoteChar, _escapeChar, _lineSeparator);
        }

        protected void _checkIndex(int index) {
            if (index < 0 || index >= _columns.size()) {
                throw new IllegalArgumentException("Illegal index "+index+"; only got "+_columns.size()+" columns");
            }
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

    protected final boolean _skipFirstDataRow;

    protected final char _columnSeparator;

    protected final int _quoteChar;
    
    protected final int _escapeChar;
    
    protected final char[] _lineSeparator;
    
    public CsvSchema(Column[] columns,
            boolean useHeader, boolean skipFirstDataRow,
            char columnSeparator, int quoteChar, int escapeChar,
            char[] lineSeparator)
    {
        if (columns == null) {
            columns = NO_COLUMNS;
        }
        _columns = columns;
        _useHeader = useHeader;
        _skipFirstDataRow = skipFirstDataRow;
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

    /**
     * Copy constructor used for creating variants using
     * <code>withXxx()</code> methods.
     */
    protected CsvSchema(Column[] columns,
            boolean useHeader, boolean skipFirstDataRow,
            char columnSeparator, int quoteChar, int escapeChar,
            char[] lineSeparator,
            Map<String,Column> columnsByName)
    {
        _columns = columns;
        _useHeader = useHeader;
        _skipFirstDataRow = skipFirstDataRow;
        _columnSeparator = columnSeparator;
        _quoteChar = quoteChar;
        _escapeChar = escapeChar;
        _lineSeparator = lineSeparator;
        _columnsByName = columnsByName;
    }    

    /**
     * Copy constructor used for creating variants using
     * <code>sortedBy()</code> methods.
     */
    protected CsvSchema(CsvSchema base, Column[] columns) {
        _columns = columns;
        _useHeader = base._useHeader;
        _skipFirstDataRow = base._skipFirstDataRow;
        _columnSeparator = base._columnSeparator;
        _quoteChar = base._quoteChar;
        _escapeChar = base._escapeChar;
        _lineSeparator = base._lineSeparator;
        _columnsByName = base._columnsByName;
    }
            
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Accessor for creating a "default" CSV schema instance, with following
     * settings:
     *<ul>
     * <li>Does NOT use header line
     *  </li>
     * <li>Uses double quotes ('"') for quoting of field values (if necessary)
     *  </li>
     * <li>Uses comma (',') as the field separator
     *  </li>
     * <li>Uses Unix linefeed ('\n') as row separator
     *  </li>
     * <li>Does NOT use any escape characters
     *  </li>
     * <li>Does NOT have any columns defined
     *  </li>
     * </ul>
     */
    public static CsvSchema emptySchema() {
        return builder().build();
    }
    
    /**
     * Helper method for constructing Builder that can be used to create modified
     * schema.
     */
    public Builder rebuild() {
        return new Builder(this);
    }

    /*
    /**********************************************************************
    /* Mutant factories
    /**********************************************************************
     */
    
    public CsvSchema withUseHeader(boolean state) {
        return (_useHeader == state) ? this
                : new CsvSchema(_columns, state, _skipFirstDataRow,
                    _columnSeparator, _quoteChar,
                    _escapeChar, _lineSeparator, _columnsByName);
    }

    /**
     * Helper method for construcing and returning schema instance that
     * is similar to this one, except that it will be using header line.
     */
    public CsvSchema withHeader() {
        return withUseHeader(true);
    }

    /**
     * Helper method for construcing and returning schema instance that
     * is similar to this one, except that it will not be using header line.
     */
    public CsvSchema withoutHeader() {
        return withUseHeader(false);
    }

    public CsvSchema withSkipFirstDataRow(boolean state) {
        return (_skipFirstDataRow == state) ? this
                : new CsvSchema(_columns, _useHeader, state,
                    _columnSeparator, _quoteChar,
                    _escapeChar, _lineSeparator, _columnsByName);
    }
    
    public CsvSchema withColumnSeparator(char sep) {
        return (_columnSeparator == sep) ? this :
            new CsvSchema(_columns, _useHeader, _skipFirstDataRow,
                    sep, _quoteChar, _escapeChar, _lineSeparator, _columnsByName);
    }

    public CsvSchema withQuoteChar(char c) {
        return (_quoteChar == c) ? this :
            new CsvSchema(_columns, _useHeader, _skipFirstDataRow,
                    _columnSeparator, c, _escapeChar, _lineSeparator, _columnsByName);
    }

    public CsvSchema withoutQuoteChar() {
        return (_quoteChar == -1) ? this :
            new CsvSchema(_columns, _useHeader, _skipFirstDataRow,
                    _columnSeparator, -1, _escapeChar, _lineSeparator, _columnsByName);
    }

    public CsvSchema withEscapeChar(char c) {
        return (_escapeChar == c) ? this
                : new CsvSchema(_columns, _useHeader, _skipFirstDataRow,
                        _columnSeparator, _quoteChar, c, _lineSeparator, _columnsByName);
    }

    public CsvSchema withoutEscapeChar() {
        return (_escapeChar == -1) ? this
                : new CsvSchema(_columns, _useHeader, _skipFirstDataRow,
                        _columnSeparator, _quoteChar, -1, _lineSeparator, _columnsByName);
    }

    public CsvSchema withLineSeparator(String sep) {
        return new CsvSchema(_columns, _useHeader, _skipFirstDataRow,
                _columnSeparator, _quoteChar, _escapeChar, sep.toCharArray(), _columnsByName);
    }

    public CsvSchema withoutColumns() {
        return new CsvSchema(NO_COLUMNS, _useHeader, _skipFirstDataRow,
                _columnSeparator, _quoteChar, _escapeChar, _lineSeparator, _columnsByName);
    }

    /**
     * Mutant factory method that will construct a new instance in which columns
     * are sorted based on names given as argument. Columns not listed in argument
     * will be sorted after those within list, using existing ordering.
     *<p>
     * For example, schema that has columns:
     *<pre>"a", "d", "c", "b"
     *</pre>
     * ordered with <code>schema.sortedBy("a", "b");</code>
     * would result instance that columns in order:
     *<pre>"a", "b", "d", "c"
     *</pre>
     * 
     * @since 2.4
     */
    public CsvSchema sortedBy(String... columnNames) {
        LinkedHashMap<String,Column> map = new LinkedHashMap<String,Column>();
        for (String colName : columnNames) {
            Column col = _columnsByName.get(colName);
            if (col != null) {
                map.put(col.getName(), col);
            }
        }
        for (Column col : _columns) {
            map.put(col.getName(), col);
        }
        return new CsvSchema(this, map.values().toArray(new Column[map.size()]));
    }

    /**
     * Mutant factory method that will construct a new instance in which columns
     * are sorted using given {@link Comparator} over column names.
     * 
     * @since 2.4
     */
    public CsvSchema sortedBy(Comparator<String> cmp) {
        TreeMap<String,Column> map = new TreeMap<String,Column>(cmp);
        for (Column col : _columns) {
            map.put(col.getName(), col);
        }
        return new CsvSchema(this, map.values().toArray(new Column[map.size()]));
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
    public boolean skipFirstDataRow() { return _skipFirstDataRow; }
    public char getColumnSeparator() { return _columnSeparator; }
    public int getQuoteChar() { return _quoteChar; }
    public int getEscapeChar() { return _escapeChar; }
    public char[] getLineSeparator() { return _lineSeparator; }

    public boolean usesQuoteChar() { return _quoteChar >= 0; }
    public boolean usesEscapeChar() { return _escapeChar >= 0; }
    
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
            .append("columns=");
        boolean first = true;
        for (Column col : _columns) {
            if (first) {
                first = false;
                sb.append('[');
            } else {
                sb.append(',');
            }
            sb.append('"');
            sb.append(col.getName());
            sb.append("\"/");
            sb.append(col.getType());
        }
        sb.append(']');
        
        sb.append(']');
        return sb.toString();
    }
}
