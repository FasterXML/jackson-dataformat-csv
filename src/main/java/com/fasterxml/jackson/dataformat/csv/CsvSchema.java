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
 * <li>nullValue (String) [default: "" (empty String)]: When asked to write Java `null`,
 *    this String value will be used instead.<br />
 *   NOTE: NOT used for reading at this point (this may change in future)
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
        Iterable<CsvSchema.Column>,
        java.io.Serializable // since 2.4.3
{
    private static final long serialVersionUID = 1L; // 2.5

    /*
    /**********************************************************************
    /* Constants, feature flags
    /**********************************************************************
     */

    protected final static int FEATURE_USE_HEADER = 0x0001;
    protected final static int FEATURE_SKIP_FIRST_DATA_ROW = 0x0002;
    protected final static int FEATURE_ALLOW_COMMENTS = 0x0004;

    protected final static int DEFAULT_FEATURES = 0;

    /*
    /**********************************************************************
    /* Constants, default settings
    /**********************************************************************
     */

    /**
     * Default separator for column values is comma (hence "Comma-Separated Values")
     */
    public final static char DEFAULT_COLUMN_SEPARATOR = ',';

    /**
     * Default separator for array elements within a column value is
     * semicolon.
     */
    public final static char DEFAULT_ARRAY_ELEMENT_SEPARATOR = ';';
    
    public final static char DEFAULT_QUOTE_CHAR = '"';

    /**
     * By default, nulls are written as empty Strings ("")
     */
    public final static char[] DEFAULT_NULL_VALUE = new char[0];
    
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
    /* Constants, other
    /**********************************************************************
     */
    
    protected final static Column[] NO_COLUMNS = new Column[0];
    
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
         *<p>
         * Note that this type allows coercion into array, if higher
         * level application calls
         * {@link com.fasterxml.jackson.core.JsonParser#isExpectedStartArrayToken},
         * unlike more explicit types.
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
         * Other non-numeric Strings may cause parsing exception.
         */
        NUMBER,

        /**
         * Value is taken to be a number (if it matches valid JSON number
         * formatting rules), literal (null, true or false) or String,
         * depending on best match.
         * Values are also trimmed (leading/trailing white space)
         */
        NUMBER_OR_STRING,

        /**
         * Value is expected to be a boolean ("true", "false") String,
         * or "null", or empty String (equivalent to null).
         * Values are trimmed (leading/trailing white space).
         * Values other than indicated above may result in an exception.
         * 
         * @since 2.5
         */
        BOOLEAN,
        
        /**
         * Value will be a multi-value sequence, separated by array element
         * separator. Element type itself may be any scalar type (that is, number
         * or String) and will not be optimized.
         * Separator may be overridden on per-column basis.
         *<p>
         * Note that this type is used for generic concept of multiple values, and
         * not specifically to match Java arrays: data-binding may match such columns
         * to {@link java.util.Collection}s as well, or even other types as necessary.
         * 
         * @since 2.5
         */
        ARRAY,
        
        ;
    }

    /**
     * Representation of info for a single column
     */
    public static class Column implements java.io.Serializable // since 2.4.3
    {
        private static final long serialVersionUID = 1L;

        public final static Column PLACEHOLDER = new Column(0, "");
        
        private final String _name;
        private final int _index;
        private final ColumnType _type;

        /**
         * @since 2.5
         */
        private final int _arrayElementSeparator;

        public Column(int index, String name) {
            this(index, name, ColumnType.STRING, -1);
        }

        public Column(int index, String name, ColumnType type) {
            this(index, name, type, -1);
        }

        public Column(int index, String name, ColumnType type, int arrayElementSep)
        {
            _index = index;
            _name = name;
            _type = type;
            _arrayElementSeparator = arrayElementSep;
        }

        public Column withName(String newName) {
            return new Column(_index, newName, _type, _arrayElementSeparator);
        }
        public Column withType(ColumnType newType) {
            return new Column(_index, _name, newType, _arrayElementSeparator);
        }
        public Column withElementSeparator(int sep) {
            return new Column(_index, _name, _type, sep);
        }
        
        public int getIndex() { return _index; }
        public String getName() { return _name; }
        public ColumnType getType() { return _type; }
        public boolean hasName(String n) {
            return (_name == n) || _name.equals(n);
        }
        
        /**
         * @since 2.5
         */
        public int getArrayElementSeparator() { return _arrayElementSeparator; }

        public boolean isArray() {
            return (_type == ColumnType.ARRAY);
        }
    }
    
    /**
     * Class used for building {@link CsvSchema} instances.
     */
    public static class Builder
    {
        protected final ArrayList<Column> _columns = new ArrayList<Column>();

        /**
         * Bitflag for general-purpose on/off features.
         * 
         * @since 2.5
         */
        protected int _features = DEFAULT_FEATURES;
        
        protected char _columnSeparator = DEFAULT_COLUMN_SEPARATOR;

        // note: need to use int to allow -1 for 'none'
        protected int _arrayElementSeparator = DEFAULT_ARRAY_ELEMENT_SEPARATOR;
        
        // note: need to use int to allow -1 for 'none'
        protected int _quoteChar = DEFAULT_QUOTE_CHAR;

        // note: need to use int to allow -1 for 'none'
        protected int _escapeChar = DEFAULT_ESCAPE_CHAR;
        
        protected char[] _lineSeparator = DEFAULT_LINEFEED;

        /**
         * @since 2.5
         */
        protected char[] _nullValue = DEFAULT_NULL_VALUE;
        
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
            _features = src._features;
            _columnSeparator = src._columnSeparator;
            _arrayElementSeparator = src._arrayElementSeparator;
            _quoteChar = src._quoteChar;
            _escapeChar = src._escapeChar;
            _lineSeparator = src._lineSeparator;
            _nullValue = src._nullValue;
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

        public Builder addArrayColumn(String name) {
            int index = _columns.size();
            return addColumn(new Column(index, name, ColumnType.ARRAY, -1));
        }
        public Builder addArrayColumn(String name, int elementSeparator) {
            int index = _columns.size();
            return addColumn(new Column(index, name, ColumnType.ARRAY, elementSeparator));
        }
        public Builder addNumberColumn(String name) {
            int index = _columns.size();
            return addColumn(new Column(index, name, ColumnType.NUMBER));
        }
        public Builder addBooleanColumn(String name) {
            int index = _columns.size();
            return addColumn(new Column(index, name, ColumnType.BOOLEAN));
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
        public void removeArrayElementSeparator(int index) {
            _checkIndex(index);
            _columns.set(index, _columns.get(index).withElementSeparator(-1));
        }
        public void setArrayElementSeparator(int index, char sep) {
            _checkIndex(index);
            _columns.set(index, _columns.get(index).withElementSeparator(sep));
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
            _feature(FEATURE_USE_HEADER, b);
            return this;
        }

        /**
         * Method for specifying whether Schema should indicate that
         * the first line that is not a header (if header handling enabled)
         * should be skipped in its entirety.
         */
        public Builder setSkipFirstDataRow(boolean b) {
            _feature(FEATURE_SKIP_FIRST_DATA_ROW, b);
            return this;
        }

        /**
         * Method for specifying whether Schema should indicate that
         * "hash comments" (lines where the first non-whitespace character
         * is '#') are allowed; if so, they will be skipped without processing.
         * 
         * @since 2.5
         */
        public Builder setAllowComments(boolean b) {
            _feature(FEATURE_ALLOW_COMMENTS, b);
            return this;
        }
        
        protected final void _feature(int feature, boolean state) {
            _features = state ? (_features | feature) : (_features & ~feature);
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
         * @since 2.5
         */
        public Builder setArrayElementSeparator(char c) {
            _arrayElementSeparator = c;
            return this;
        }

        /**
         * @since 2.5
         */
        public Builder disableElementSeparator(char c) {
            _arrayElementSeparator = -1;
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

        public Builder setNullValue(String nvl) {
            return setNullValue(nvl.toCharArray());
        }

        public Builder setNullValue(char[] nvl) {
            _nullValue = (nvl == null) ? DEFAULT_NULL_VALUE : nvl;
            return this;
        }
        
        public CsvSchema build()
        {
            Column[] cols = _columns.toArray(new Column[_columns.size()]);
            return new CsvSchema(cols, _features,
                    _columnSeparator, _quoteChar, _escapeChar,
                    _lineSeparator, _arrayElementSeparator,
                    _nullValue);
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

    /**
     * Bitflag for general-purpose on/off features.
     * 
     * @since 2.5
     */
    protected int _features = DEFAULT_FEATURES;

    protected final char _columnSeparator;

    protected final int _arrayElementSeparator;
    
    protected final int _quoteChar;
    
    protected final int _escapeChar;
    
    protected final char[] _lineSeparator;

    /**
     * @since 2.5
     */
    protected final char[] _nullValue;

    @Deprecated // in 2.5; remove from 2.6
    public CsvSchema(Column[] columns,
            boolean useHeader, boolean skipFirstDataRow,
            char columnSeparator, int quoteChar, int escapeChar,
            char[] lineSeparator)
    {
        this(columns,
                (useHeader ? FEATURE_USE_HEADER : 0) + (skipFirstDataRow ? FEATURE_SKIP_FIRST_DATA_ROW : 0),
                columnSeparator, quoteChar, escapeChar, lineSeparator,
                DEFAULT_ARRAY_ELEMENT_SEPARATOR, DEFAULT_NULL_VALUE);
    }

    /**
     * @since 2.5
     */
    public CsvSchema(Column[] columns, int features,
            char columnSeparator, int quoteChar, int escapeChar,
            char[] lineSeparator, int arrayElementSeparator,
            char[] nullValue)
    {
        if (columns == null) {
            columns = NO_COLUMNS;
        }
        _columns = columns;
        _features = features;
        _columnSeparator = columnSeparator;
        _arrayElementSeparator = arrayElementSeparator;
        _quoteChar = quoteChar;
        _escapeChar = escapeChar;
        _lineSeparator = lineSeparator;
        _nullValue = nullValue;
        
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
    protected CsvSchema(Column[] columns, int features,
            char columnSeparator, int quoteChar, int escapeChar,
            char[] lineSeparator, int arrayElementSeparator,
            char[] nullValue,
            Map<String,Column> columnsByName)
    {
        _columns = columns;
        _features = features;
        _columnSeparator = columnSeparator;
        _quoteChar = quoteChar;
        _escapeChar = escapeChar;
        _lineSeparator = lineSeparator;
        _arrayElementSeparator = arrayElementSeparator;
        _nullValue = nullValue;
        _columnsByName = columnsByName;
    }    

    /**
     * Copy constructor used for creating variants using
     * <code>sortedBy()</code> methods.
     */
    protected CsvSchema(CsvSchema base, Column[] columns) {
        _columns = columns;
        _features = base._features;
        _columnSeparator = base._columnSeparator;
        _quoteChar = base._quoteChar;
        _escapeChar = base._escapeChar;
        _lineSeparator = base._lineSeparator;
        _arrayElementSeparator = base._arrayElementSeparator;
        _nullValue = base._nullValue;
        _columnsByName = base._columnsByName;
    }

    /**
     * Copy constructor used for creating variants for on/off features
     * 
     * @since 2.5
     */
    protected CsvSchema(CsvSchema base, int features) {
        _columns = base._columns;
        _features = features;
        _columnSeparator = base._columnSeparator;
        _quoteChar = base._quoteChar;
        _escapeChar = base._escapeChar;
        _lineSeparator = base._lineSeparator;
        _arrayElementSeparator = base._arrayElementSeparator;
        _nullValue = base._nullValue;
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
        return _withFeature(FEATURE_USE_HEADER, state);
    }

    /**
     * Helper method for constructing and returning schema instance that
     * is similar to this one, except that it will be using header line.
     */
    public CsvSchema withHeader() {
        return _withFeature(FEATURE_USE_HEADER, true);
    }

    /**
     * Helper method for construcing and returning schema instance that
     * is similar to this one, except that it will not be using header line.
     */
    public CsvSchema withoutHeader() {
        return _withFeature(FEATURE_USE_HEADER, false);
    }

    public CsvSchema withSkipFirstDataRow(boolean state) {
        return _withFeature(FEATURE_SKIP_FIRST_DATA_ROW, state);
    }

    /**
     * Method to indicate whether "hash comments" are allowed
     * for document described by this schema.
     * 
     * @since 2.5
     */
    public CsvSchema withAllowComments(boolean state) {
        return _withFeature(FEATURE_ALLOW_COMMENTS, state);
    }

    /**
     * Method to indicate that "hash comments" ARE allowed
     * for document described by this schema.
     * 
     * @since 2.5
     */
    public CsvSchema withComments() {
        return _withFeature(FEATURE_ALLOW_COMMENTS, true);
    }

    /**
     * Method to indicate that "hash comments" are NOT allowed for document
     * described by this schema.
     * 
     * @since 2.5
     */
    public CsvSchema withoutComments() {
        return _withFeature(FEATURE_ALLOW_COMMENTS, false);
    }

    protected CsvSchema _withFeature(int feature, boolean state) {
        int newFeatures = state ? (_features | feature) : (_features & ~feature);
        return (newFeatures == _features) ? this : new CsvSchema(this, newFeatures);
    }

    public CsvSchema withColumnSeparator(char sep) {
        return (_columnSeparator == sep) ? this :
            new CsvSchema(_columns, _features,
                    sep, _quoteChar, _escapeChar, _lineSeparator, _arrayElementSeparator,
                    _nullValue, _columnsByName);
    }

    public CsvSchema withQuoteChar(char c) {
        return (_quoteChar == c) ? this :
            new CsvSchema(_columns, _features,
                    _columnSeparator, c, _escapeChar, _lineSeparator,_arrayElementSeparator,
                    _nullValue, _columnsByName);
    }

    public CsvSchema withoutQuoteChar() {
        return (_quoteChar == -1) ? this :
            new CsvSchema(_columns, _features,
                    _columnSeparator, -1, _escapeChar, _lineSeparator, _arrayElementSeparator,
                    _nullValue, _columnsByName);
    }

    public CsvSchema withEscapeChar(char c) {
        return (_escapeChar == c) ? this
                : new CsvSchema(_columns, _features,
                        _columnSeparator, _quoteChar, c, _lineSeparator, _arrayElementSeparator,
                        _nullValue, _columnsByName);
    }

    public CsvSchema withoutEscapeChar() {
        return (_escapeChar == -1) ? this
                : new CsvSchema(_columns, _features,
                        _columnSeparator, _quoteChar, -1, _lineSeparator, _arrayElementSeparator,
                        _nullValue, _columnsByName);
    }

    /**
     * @since 2.5
     */
    public CsvSchema withArrayElementSeparator(char c) {
        return (_arrayElementSeparator == c) ? this
                : new CsvSchema(_columns, _features,
                        _columnSeparator, _quoteChar, _escapeChar, _lineSeparator, c,
                        _nullValue, _columnsByName);
    }

    /**
     * @since 2.5
     */
    public CsvSchema withoutArrayElementSeparator() {
        return (_arrayElementSeparator == -1) ? this
                : new CsvSchema(_columns, _features,
                        _columnSeparator, _quoteChar, _escapeChar, _lineSeparator, -1,
                        _nullValue, _columnsByName);
    }
    
    public CsvSchema withLineSeparator(String sep) {
        return new CsvSchema(_columns, _features,
                _columnSeparator, _quoteChar, _escapeChar, sep.toCharArray(),
                _arrayElementSeparator, _nullValue, _columnsByName);
    }

    /**
     * @since 2.5
     */
    public CsvSchema withNullValue(String nvl) {
        return new CsvSchema(_columns, _features,
                _columnSeparator, _quoteChar, _escapeChar, _lineSeparator,
                _arrayElementSeparator,
                (nvl == null) ? DEFAULT_NULL_VALUE : nvl.toCharArray(),
                _columnsByName);
    }
    
    public CsvSchema withoutColumns() {
        return new CsvSchema(NO_COLUMNS, _features,
                _columnSeparator, _quoteChar, _escapeChar, _lineSeparator, _arrayElementSeparator,
                _nullValue, _columnsByName);
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

    public boolean usesHeader() { return (_features & FEATURE_USE_HEADER) != 0; }
    public boolean skipsFirstDataRow() { return (_features & FEATURE_SKIP_FIRST_DATA_ROW) != 0; }
    public boolean allowsComments() { return (_features & FEATURE_ALLOW_COMMENTS) != 0; }

    /**
     * @deprecated Use {@link #usesHeader()} instead
     */
    @Deprecated // since 2.5
    public boolean useHeader() { return (_features & FEATURE_USE_HEADER) != 0; }

    /**
     * @deprecated Use {@link #skipsFirstDataRow()} instead
     */
    @Deprecated // since 2.5
    public boolean skipFirstDataRow() { return (_features & FEATURE_SKIP_FIRST_DATA_ROW) != 0; }
    
    public char getColumnSeparator() { return _columnSeparator; }
    public int getArrayElementSeparator() { return _arrayElementSeparator; }
    public int getQuoteChar() { return _quoteChar; }
    public int getEscapeChar() { return _escapeChar; }

    public char[] getLineSeparator() { return _lineSeparator; }

    /**
     * @since 2.5
     */
    public char[] getNullValue() { return _nullValue; }

    public boolean usesQuoteChar() { return _quoteChar >= 0; }
    public boolean usesEscapeChar() { return _escapeChar >= 0; }

    /**
     * @since 2.5
     */
    public boolean hasArrayElementSeparator() { return _arrayElementSeparator >= 0; }
    
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
     * Optimized variant where a hint is given as to likely index of the column
     * name.
     *
     * @since 2.6
     */
    public Column column(String name, int probableIndex) {
        if (probableIndex < _columns.length) {
            Column col = _columns[probableIndex];
            if (col.hasName(name)) {
                return col;
            }
        }
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
            .append("columns=[");
        boolean first = true;
        for (Column col : _columns) {
            if (first) {
                first = false;
            } else {
                sb.append(',');
            }
            sb.append('"');
            sb.append(col.getName());
            sb.append("\"/");
            sb.append(col.getType());
        }
        sb.append(']');
        sb.append(", header? ").append(usesHeader());
        sb.append(", skipFirst? ").append(skipsFirstDataRow());
        sb.append(", comments?? ").append(allowsComments());
        
        sb.append(']');
        return sb.toString();
    }
}
