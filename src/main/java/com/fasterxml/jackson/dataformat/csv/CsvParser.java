package com.fasterxml.jackson.dataformat.csv;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.base.ParserMinimalBase;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.json.DupDetector;
import com.fasterxml.jackson.core.json.JsonReadContext;
import com.fasterxml.jackson.core.util.BufferRecycler;
import com.fasterxml.jackson.core.util.ByteArrayBuilder;
import com.fasterxml.jackson.dataformat.csv.impl.CsvDecoder;
import com.fasterxml.jackson.dataformat.csv.impl.TextBuffer;

/**
 * {@link JsonParser} implementation used to expose CSV documents
 * in form that allows other Jackson functionality to deal
 * with it.
 *<p>
 * Implementation is based on a state-machine that pulls information
 * using {@link CsvDecoder}.
 */
public class CsvParser
    extends ParserMinimalBase
{
    /**
     * Enumeration that defines all togglable features for CSV parsers
     */
    public enum Feature {
        /**
         * Feature determines whether spaces around separator characters
         * (commas) are to be automatically trimmed before being reported
         * or not.
         * Note that this does NOT force trimming of possible white space from
         * within double-quoted values, but only those surrounding unquoted
         * values (white space outside of double-quotes is never included regardless
         * of trimming).
         *<p>
         * Default value is false, as per <a href="http://tools.ietf.org/html/rfc4180">RFC-4180</a>.
         */
        TRIM_SPACES(false),
        
        /**
         * Feature that determines how stream of records (usually CSV lines, but sometimes
         * multiple lines when linefeeds are included in quoted values) is exposed:
         * either as a sequence of Objects (false), or as an array of Objects (true).
         * Using stream of Objects is convenient when using
         * <code>ObjectMapper.readValues(...)</code>
         * and array of Objects convenient when binding to <code>List</code>s or
         * arrays of values.
         *<p>
         * Default value is false, meaning that by default a CSV document is exposed as
         * a sequence of root-level Object entries.
         */
        WRAP_AS_ARRAY(false)
        ;

        final boolean _defaultState;
        final int _mask;
        
        /**
         * Method that calculates bit set (flags) of all features that
         * are enabled by default.
         */
        public static int collectDefaults()
        {
            int flags = 0;
            for (Feature f : values()) {
                if (f.enabledByDefault()) {
                    flags |= f.getMask();
                }
            }
            return flags;
        }
        
        private Feature(boolean defaultState) {
            _defaultState = defaultState;
            _mask = (1 << ordinal());
        }
        
        public boolean enabledIn(int flags) { return (flags & _mask) != 0; }
        public boolean enabledByDefault() { return _defaultState; }
        public int getMask() { return _mask; }
    }

    private final static CsvSchema EMPTY_SCHEMA;
    static {
        EMPTY_SCHEMA = CsvSchema.emptySchema();
    }

    /*
    /**********************************************************************
    /* State constants
    /**********************************************************************
     */

    /**
     * Initial state before anything is read from document.
     */
    protected final static int STATE_DOC_START = 0;
    
    /**
     * State before logical start of a record, in which next
     * token to return will be {@link JsonToken#START_OBJECT}
     * (or if no Schema is provided, {@link JsonToken#START_ARRAY}).
     */
    protected final static int STATE_RECORD_START = 1;

    /**
     * State in which next entry will be available, returning
     * either {@link JsonToken#FIELD_NAME} or value
     * (depending on whether entries are expressed as
     * Objects or just Arrays); or
     * matching close marker.
     */
    protected final static int STATE_NEXT_ENTRY = 2;

    /**
     * State in which value matching field name will
     * be returned.
     */
    protected final static int STATE_NAMED_VALUE = 3;

    /**
     * State in which "unnamed" value (entry in an array)
     * will be returned, if one available; otherwise
     * end-array is returned.
     */
    protected final static int STATE_UNNAMED_VALUE = 4;

    /**
     * State in which a column value has been determined to be of
     * an array type, and will need to be split into multiple
     * values. This can currently only occur for named values.
     * 
     * @since 2.5
     */
    protected final static int STATE_IN_ARRAY = 5;

    /**
     * State in which end marker is returned; either
     * null (if no array wrapping), or
     * {@link JsonToken#END_ARRAY} for wrapping.
     * This step will loop, returning series of nulls
     * if {@link #nextToken} is called multiple times.
     */
    protected final static int STATE_DOC_END = 6;

    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */
    
    /**
     * Codec used for data binding when (if) requested.
     */
    protected ObjectCodec _objectCodec;

    protected int _formatFeatures;
    
    /**
     * Definition of columns being read. Initialized to "empty" instance, which
     * has default configuration settings.
     */
    protected CsvSchema _schema = EMPTY_SCHEMA;

    /**
     * Number of columns defined by schema.
     */
    protected int _columnCount = 0;

    /*
    /**********************************************************************
    /* State
    /**********************************************************************
     */
    
    /**
     * Information about parser context, context in which
     * the next token is to be parsed (root, array, object).
     */
    protected JsonReadContext _parsingContext;

    /**
     * Name of column that we exposed most recently, accessible after
     * {@link JsonToken#FIELD_NAME} as well as value tokens immediately
     * following field name.
     */
    protected String _currentName;

    /**
     * String value for the current column, if accessed.
     */
    protected String _currentValue;
    
    /**
     * Index of the column we are exposing
     */
    protected int _columnIndex;
    
    /**
     * Current logical state of the parser; one of <code>STATE_</code>
     * constants.
     */
    protected int _state = STATE_DOC_START;
    
    /**
     * We will hold on to decoded binary data, for duration of
     * current event, so that multiple calls to
     * {@link #getBinaryValue} will not need to decode data more
     * than once.
     */
    protected byte[] _binaryValue;

    /**
     * Pointer to the first character of the next array value to return.
     */
    protected int _arrayValueStart;

    /**
     * Contents of the cell, to be split into distinct array values.
     */
    protected String _arrayValue;

    protected char _arraySeparator;
    
    /*
    /**********************************************************************
    /* Helper objects
    /**********************************************************************
     */

    /**
     * Thing that actually reads the CSV content
     */
    protected final CsvDecoder _reader;

    /**
     * Buffer that contains contents of all values after processing
     * of doubled-quotes, escaped characters.
     */
    protected final TextBuffer _textBuffer;
    
    protected ByteArrayBuilder _byteArrayBuilder;
    
    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */
    
    public CsvParser(IOContext ctxt, BufferRecycler br,
            int parserFeatures, int csvFeatures,
            ObjectCodec codec, Reader reader)
    {
        super(parserFeatures);    
        _objectCodec = codec;
        _textBuffer = new TextBuffer(br);
        DupDetector dups = JsonParser.Feature.STRICT_DUPLICATE_DETECTION.enabledIn(parserFeatures)
                ? DupDetector.rootDetector(this) : null;
        _formatFeatures = csvFeatures;
        _parsingContext = JsonReadContext.createRootContext(dups);
        _reader = new CsvDecoder(this, ctxt, reader, _schema, _textBuffer,
                isEnabled(JsonParser.Feature.AUTO_CLOSE_SOURCE),
                isEnabled(Feature.TRIM_SPACES));
    }

    /*
    /**********************************************************
    /* Versioned                                                                             
    /**********************************************************                 
     */

    @Override
    public Version version() {
        return PackageVersion.VERSION;
    }

    /*
    /**********************************************************                              
    /* Overridden methods
    /**********************************************************                              
     */
    
    @Override
    public ObjectCodec getCodec() {
        return _objectCodec;
    }

    @Override
    public void setCodec(ObjectCodec c) {
        _objectCodec = c;
    }
    
    @Override
    public boolean canUseSchema(FormatSchema schema) {
        return (schema instanceof CsvSchema);
    }

    @Override
    public void setSchema(FormatSchema schema)
    {
        if (schema instanceof CsvSchema) {
            _schema = (CsvSchema) schema;
        } else if (schema == null) {
            schema = EMPTY_SCHEMA;
        } else {
            super.setSchema(schema);
        }
        _columnCount = _schema.size();            
        _reader.setSchema(_schema);
    }

    @Override
    public int releaseBuffered(Writer out) throws IOException {
        return _reader.releaseBuffered(out);
    }

    @Override
    public boolean isClosed() { return _reader.isClosed(); }

    @Override
    public void close() throws IOException { _reader.close(); }

    /*
    /***************************************************
    /* Public API, configuration
    /***************************************************
     */

    /**
     * Method for enabling specified CSV feature
     * (check {@link Feature} for list of features)
     */
    public JsonParser enable(Feature f)
    {
        _formatFeatures |= f.getMask();
        return this;
    }

    /**
     * Method for disabling specified  CSV feature
     * (check {@link Feature} for list of features)
     */
    public JsonParser disable(Feature f)
    {
        _formatFeatures &= ~f.getMask();
        return this;
    }

    /**
     * Method for enabling or disabling specified CSV feature
     * (check {@link Feature} for list of features)
     */
    public JsonParser configure(Feature f, boolean state)
    {
        if (state) {
            enable(f);
        } else {
            disable(f);
        }
        return this;
    }

    /**
     * Method for checking whether specified CSV {@link Feature}
     * is enabled.
     */
    public boolean isEnabled(Feature f) {
        return (_formatFeatures & f.getMask()) != 0;
    }

    /**
     * Accessor for getting active schema definition: it may be
     * "empty" (no column definitions), but will never be null
     * since it defaults to an empty schema (and default configuration)
     */
    @Override
    public CsvSchema getSchema() {
        return _schema;
    }
    
    /*
    /**********************************************************
    /* Location info
    /**********************************************************
     */
    
    @Override
    public JsonStreamContext getParsingContext() {
        return _parsingContext;
    }

    @Override
    public JsonLocation getTokenLocation() {
        return _reader.getTokenLocation();
    }

    @Override
    public JsonLocation getCurrentLocation() {
        return _reader.getCurrentLocation();
    }

    @Override
    public Object getInputSource() {
        return _reader.getInputSource();
    }
    
    /*
    /**********************************************************
    /* Parsing
    /**********************************************************
     */

    /**
     * We need to override this method to support coercion from basic
     * String value into array, in cases where schema does not
     * specify actual type.
     */
    @Override
    public boolean isExpectedStartArrayToken() {
        if (_currToken == null) {
            return false;
        }
        switch (_currToken.id()) {
        case JsonTokenId.ID_FIELD_NAME:
        case JsonTokenId.ID_START_OBJECT:
        case JsonTokenId.ID_END_OBJECT:
        case JsonTokenId.ID_END_ARRAY:
            return false;
        case JsonTokenId.ID_START_ARRAY:
            return true;
        }
        // Otherwise: may coerce into array, iff we have essentially "untyped" column
        CsvSchema.Column column = _schema.column(_columnIndex);
        if (column.getType() == CsvSchema.ColumnType.STRING) {
            _startArray(column);
            return true;
        }
        return false;
    }

    @Override
    public String getCurrentName() throws IOException {
        return _currentName;
    }

    @Override
    public void overrideCurrentName(String name) {
        _currentName = name;
    }
    
    @Override
    public JsonToken nextToken() throws IOException
    {
        _binaryValue = null;
        switch (_state) {
        case STATE_DOC_START:
            return (_currToken = _handleStartDoc());
        case STATE_RECORD_START:
            return (_currToken = _handleRecordStart());
        case STATE_NEXT_ENTRY:
            return (_currToken = _handleNextEntry());
        case STATE_NAMED_VALUE:
            return (_currToken = _handleNamedValue());
        case STATE_UNNAMED_VALUE:
            return (_currToken = _handleUnnamedValue());
        case STATE_IN_ARRAY:
            return (_currToken = _handleArrayValue());
        case STATE_DOC_END:
            _reader.close();
            if (_parsingContext.inRoot()) {
                return null;
            }
            // should always be in array, actually... but:
            boolean inArray = _parsingContext.inArray();
            _parsingContext = _parsingContext.getParent();
            return inArray ? JsonToken.END_ARRAY : JsonToken.END_OBJECT;
        default:
            throw new IllegalStateException();
        }
    }

    /*
    /**********************************************************
    /* Parsing, helper methods
    /**********************************************************
     */
    
    /**
     * Method called to handle details of initializing things to return
     * the very first token.
     */
    protected JsonToken _handleStartDoc() throws IOException
    {
        // First things first: are we expecting header line? If so, read, process
        if (_schema.useHeader()) {
            _readHeaderLine();
        }
        // and if we are to skip the first data line, skip it
        if (_schema.skipFirstDataRow()) {
            _reader.skipLine();
        }
        
        /* Only one real complication, actually; empy documents (zero bytes).
         * Those have no entries. Should be easy enough to detect like so:
         */
        final boolean wrapAsArray = Feature.WRAP_AS_ARRAY.enabledIn(_formatFeatures);
        if (!_reader.hasMoreInput()) {
            _state = STATE_DOC_END;
            // but even empty sequence must still be wrapped in logical array
            if (wrapAsArray) {
                _parsingContext = _reader.childArrayContext(_parsingContext);
                return JsonToken.START_ARRAY;
            }
            return null;
        }
        
        if (wrapAsArray) {
            _parsingContext = _reader.childArrayContext(_parsingContext);
            _state = STATE_RECORD_START;
            return JsonToken.START_ARRAY;
        }
        // otherwise, same as regular new entry...
        return _handleRecordStart();
    }

    protected JsonToken _handleRecordStart() throws IOException
    {
        _columnIndex = 0;
        if (_columnCount == 0) { // no schema; exposed as an array
            _state = STATE_UNNAMED_VALUE;
            _parsingContext = _reader.childArrayContext(_parsingContext);
            return JsonToken.START_ARRAY;
        }
        // otherwise, exposed as an Object
        _parsingContext = _reader.childObjectContext(_parsingContext);
        _state = STATE_NEXT_ENTRY;
        return JsonToken.START_OBJECT;
    }

    protected JsonToken _handleNextEntry() throws IOException
    {
        // NOTE: only called when we do have real Schema
        String next = _reader.nextString();

        if (next == null) { // end of record or input...
            _parsingContext = _parsingContext.getParent();
            // let's handle EOF or linefeed
            if (!_reader.startNewLine()) {
                _state = STATE_DOC_END;
            } else {
                // no, just end of record
                _state = STATE_RECORD_START;
            }
            return JsonToken.END_OBJECT;
        }
        _state = STATE_NAMED_VALUE;
        _currentValue = next;
        if (_columnIndex >= _columnCount) {
            _currentName = null;
            /* 14-Mar-2012, tatu: As per [Issue-1], let's allow one specific
             *  case of extra: if we get just one all-whitespace entry, that
             *  can be just skipped
             */
            if (_columnIndex == _columnCount) {
                next = next.trim();
                if (next.length() == 0) {
                    /* if so, need to verify we then get the end-of-record;
                     * easiest to do by just calling ourselves again...
                     */
                    return _handleNextEntryExpectEOL();
                }
            }
            _reportError("Too many entries: expected at most "+_columnCount+" (value #"+_columnCount+" ("+next.length()+" chars) \""+next+"\")");
        }
        _currentName = _schema.column(_columnIndex).getName();
        return JsonToken.FIELD_NAME;
    }

    protected JsonToken _handleNextEntryExpectEOL() throws IOException
    {
        String next = _reader.nextString();

        if (next != null) { // should end of record or input
            _reportError("Too many entries: expected at most "+_columnCount+" (value #"+_columnCount+" ("+next.length()+" chars) \""+next+"\")");
        }
        _parsingContext = _parsingContext.getParent();
        if (!_reader.startNewLine()) {
            _state = STATE_DOC_END;
        } else {
            _state = STATE_RECORD_START;
        }
        return JsonToken.END_OBJECT;
    }
    
    protected JsonToken _handleNamedValue() throws IOException
    {
        CsvSchema.Column column = _schema.column(_columnIndex);
        ++_columnIndex;
        if (column.isArray()) {
            _startArray(column);
            return JsonToken.START_ARRAY;
        }
        _state = STATE_NEXT_ENTRY;
        return JsonToken.VALUE_STRING;
    }

    protected JsonToken _handleUnnamedValue() throws IOException
    {
        String next = _reader.nextString();
        if (next == null) { // end of record or input...
            _parsingContext = _parsingContext.getParent();
            if (!_reader.startNewLine()) { // end of whole thing...
                _state = STATE_DOC_END;
            } else {
                // no, just end of record
                _state = STATE_RECORD_START;
            }
            return JsonToken.END_ARRAY;
        }
        // state remains the same
        _currentValue = next;
        ++_columnIndex;
        return JsonToken.VALUE_STRING;
    }

    protected JsonToken _handleArrayValue() throws IOException
    {
        int offset = _arrayValueStart;
        if (offset < 0) { // just returned last value
            _parsingContext = _parsingContext.getParent();
            // no arrays in arrays (at least for now), so must be back to named value
            _state = STATE_NEXT_ENTRY;
             return JsonToken.END_ARRAY;
        }
        int end = _arrayValue.indexOf(_arraySeparator, offset);
        if (end < 0) { // last value
            _currentValue = (offset == 0) ? _arrayValue : _arrayValue.substring(offset);
            _arrayValueStart = end;
        } else {
            _currentValue = _arrayValue.substring(offset, end);
            _arrayValueStart = end+1;
        }
        return JsonToken.VALUE_STRING;
    }

    /**
     * Method called to process the expected header line
     */
    protected void _readHeaderLine() throws IOException
    {
        /* Two separate cases:
         * 
         * (a) We already have a Schema with columns; if so, header will be skipped
         * (b) Otherwise, need to find column definitions; empty one is not acceptable
         */

        if (_schema.size() > 0) { // case (a); skip all/any
            while (_reader.nextString() != null) { }
            return;
        }
        // case (b); read all
        String name;
        // base setting on existing schema, but drop columns
        CsvSchema.Builder builder = _schema.rebuild().clearColumns();
        
        while ((name = _reader.nextString()) != null) {
            // one more thing: always trim names, regardless of config settings
            name = name.trim();
            
            // See if "old" schema defined type; if so, use that type...
            CsvSchema.Column prev = _schema.column(name);
            if (prev != null) {
                builder.addColumn(name, prev.getType());
            } else {
                builder.addColumn(name);
            }
        }
        // Ok: did we get any  columns?
        CsvSchema newSchema = builder.build();
        int size = newSchema.size();
        if (size < 2) { // 1 just because we may get 'empty' header name
            String first = (size == 0) ? "" : newSchema.column(0).getName().trim();
            if (first.length() == 0) {
                _reportError("Empty header line: can not bind data");
            }
        }
        // otherwise we will use what we got
        setSchema(builder.build());
    }

    /*
    /**********************************************************
    /* String value handling
    /**********************************************************
     */

    // For now we do not store char[] representation...
    @Override
    public boolean hasTextCharacters() {
        if (_currToken == JsonToken.FIELD_NAME) {
            return false;
        }
        return _textBuffer.hasTextAsCharacters();
    }

    @Override
    public String getText() throws IOException {
        if (_currToken == JsonToken.FIELD_NAME) {
            return _currentName;
        }
        return _currentValue;
    }

    @Override
    public char[] getTextCharacters() throws IOException {
        if (_currToken == JsonToken.FIELD_NAME) {
            return _currentName.toCharArray();
        }
        return _textBuffer.contentsAsArray();
    }

    @Override
    public int getTextLength() throws IOException {
        if (_currToken == JsonToken.FIELD_NAME) {
            return _currentName.length();
        }
        return _textBuffer.size();
    }

    @Override
    public int getTextOffset() throws IOException {
        return 0;
    }

    /*
    /**********************************************************************
    /* Binary (base64)
    /**********************************************************************
     */

    @Override
    public Object getEmbeddedObject() throws IOException {
        // in theory may access binary data using this method so...
        return _binaryValue;
    }

    @SuppressWarnings("resource")
    @Override
    public byte[] getBinaryValue(Base64Variant variant) throws IOException
    {
        if (_binaryValue == null) {
            if (_currToken != JsonToken.VALUE_STRING) {
                _reportError("Current token ("+_currToken+") not VALUE_STRING, can not access as binary");
            }
            ByteArrayBuilder builder = _getByteArrayBuilder();
            _decodeBase64(_currentValue, builder, variant);
            _binaryValue = builder.toByteArray();
        }
        return _binaryValue;
    }

    /*
    /**********************************************************************
    /* Number accessors
    /**********************************************************************
     */

    @Override
    public NumberType getNumberType() throws IOException {
        return _reader.getNumberType();
    }
    
    @Override
    public Number getNumberValue() throws IOException {
        return _reader.getNumberValue();
    }

    @Override
    public int getIntValue() throws IOException {
        return _reader.getIntValue();
    }
    
    @Override
    public long getLongValue() throws IOException {
        return _reader.getLongValue();
    }

    @Override
    public BigInteger getBigIntegerValue() throws IOException {
        return _reader.getBigIntegerValue();
    }

    @Override
    public float getFloatValue() throws IOException {
        return _reader.getFloatValue();
    }

    @Override
    public double getDoubleValue() throws IOException {
        return _reader.getDoubleValue();
    }

    @Override
    public BigDecimal getDecimalValue() throws IOException {
        return _reader.getDecimalValue();
    }

    /*
    /**********************************************************************
    /* Helper methods from base class
    /**********************************************************************
     */
    
    @Override
    protected void _handleEOF() throws JsonParseException {
        // I don't think there's problem with EOFs usually; except maybe in quoted stuff?
        _reportInvalidEOF(": expected closing quote character");
    }

    /*
    /**********************************************************************
    /* Helper methods for CsvReader
    /**********************************************************************
     */

    // must be (re)defined to make package-accessible
    public void _reportCsvError(String msg)  throws JsonParseException {
        super._reportError(msg);
    }

    public void _reportUnexpectedCsvChar(int ch, String msg)  throws JsonParseException {
        super._reportUnexpectedChar(ch, msg);
    }
    
    /*
    /**********************************************************************
    /* Internal methods
    /**********************************************************************
     */
    
    public ByteArrayBuilder _getByteArrayBuilder()
    {
        if (_byteArrayBuilder == null) {
            _byteArrayBuilder = new ByteArrayBuilder();
        } else {
            _byteArrayBuilder.reset();
        }
        return _byteArrayBuilder;
    }

    protected void _startArray(CsvSchema.Column column)
    {
        _currToken = JsonToken.START_ARRAY;
        _parsingContext = _parsingContext.createChildArrayContext(_reader.getCurrentRow(),
                _reader.getCurrentColumn());
        _state = STATE_IN_ARRAY;
        _arrayValueStart = 0;
        _arrayValue = _currentValue;
        int sep = column.getArrayElementSeparator();
        if (sep <= 0) {
            sep = _schema.getArrayElementSeparator();
        }
        _arraySeparator = (char) sep;
    }
}
