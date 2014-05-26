package com.fasterxml.jackson.dataformat.csv;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.base.GeneratorBase;
import com.fasterxml.jackson.core.json.JsonWriteContext;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.dataformat.csv.impl.CsvWriter;

public class CsvGenerator extends GeneratorBase
{
    /**
     * Enumeration that defines all togglable features for CSV writers
     * (if any: currently none)
     */
    public enum Feature {
        /**
         * Feature that determines how much work is done before determining that
         * a column value requires quoting: when set as <code>true</code>, full
         * check is made to only use quoting when it is strictly necessary;
         * but when <code>false</code>, a faster but more conservative check
         * is made, and possibly quoting is used for values that might not need it.
         * Trade-offs is basically between optimal/minimal quoting (true), and
         * faster handling (false).
         * Faster check involves only checking first N characters of value, as well
         * as possible looser checks.
         *<p>
         * Note, however, that regardless setting, all values that need to be quoted
         * will be: it is just that when set to <code>false</code>, other values may
         * also be quoted (to avoid having to do more expensive checks).
         *<p>
         * Default value is <code>false</code> for "loose" (approximate, conservative)
         * checking.
         * 
         * @since 2.4
         */
        STRICT_CHECK_FOR_QUOTING(false),

        /**
         * Feature that determines whether columns without matching value may be omitted,
         * when they are the last values of the row.
         * If <code>true</code>, values and separators between values may be omitted, to slightly reduce
         * length of the row; if <code>false</code>, separators need to stay in place and values
         * are indicated by empty Strings.
         * 
         * @since 2.4
         */
        OMIT_MISSING_TAIL_COLUMNS(false),
        ;

        protected final boolean _defaultState;
        protected final int _mask;
        
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
        
        public boolean enabledIn(int flags) { return (flags & getMask()) != 0; }
        public boolean enabledByDefault() { return _defaultState; }
        public int getMask() { return _mask; }
    }

    protected final static long MIN_INT_AS_LONG = Integer.MIN_VALUE;
    protected final static long MAX_INT_AS_LONG = Integer.MAX_VALUE;
    
    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    private final static CsvSchema EMPTY_SCHEMA;
    static {
        EMPTY_SCHEMA = CsvSchema.emptySchema();
    }
    
    final protected IOContext _ioContext;

    /**
     * Bit flag composed of bits that indicate which
     * {@link CsvGenerator.Feature}s
     * are enabled.
     */
    protected int _csvFeatures;

    /**
     * Definition of columns being written, if available.
     */
    protected CsvSchema _schema = EMPTY_SCHEMA;

    // note: can not be final since we may need to re-create it for new schema
    protected CsvWriter _writer;
    
    /*
    /**********************************************************
    /* Output state
    /**********************************************************
     */

    /**
     * Flag that indicates that we need to write header line, if
     * one is needed. Used because schema may be specified after
     * instance is constructed.
     */
    protected boolean _handleFirstLine = true;
    
    /**
     * Index of column that we will be getting next, based on
     * field name call that was made.
     */
    protected int _nextColumnByName = -1;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    /**
     * @deprecated Since 2.4
     */
    @Deprecated
    public CsvGenerator(IOContext ctxt, int jsonFeatures, int csvFeatures,
            ObjectCodec codec, Writer out,
            char columnSeparator, char quoteChar, char[] linefeed)
    {
        this(ctxt, jsonFeatures, csvFeatures, codec,
                new CsvWriter(ctxt, csvFeatures, out, columnSeparator, quoteChar, linefeed));
    }

    /**
     * @since 2.4
     */
    public CsvGenerator(IOContext ctxt, int jsonFeatures, int csvFeatures,
            ObjectCodec codec, Writer out, CsvSchema schema)
    {
        super(jsonFeatures, codec);
        _ioContext = ctxt;
        _csvFeatures = csvFeatures;
        _schema = schema;
        _writer = new CsvWriter(ctxt, csvFeatures, out, schema);
    }

    public CsvGenerator(IOContext ctxt, int jsonFeatures, int csvFeatures,
            ObjectCodec codec, CsvWriter csvWriter)
    {
        super(jsonFeatures, codec);
        _ioContext = ctxt;
        _csvFeatures = csvFeatures;
        _writer = csvWriter;
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
    /* Overridden methods, configuration
    /**********************************************************
     */

    /**
     * No way (or need) to indent anything, so let's block any attempts.
     * (should we throw an exception instead?)
     */
    @Override
    public CsvGenerator useDefaultPrettyPrinter() {
        return this;
    }

    /**
     * No way (or need) to indent anything, so let's block any attempts.
     * (should we throw an exception instead?)
     */
    @Override
    public CsvGenerator setPrettyPrinter(PrettyPrinter pp) {
        return this;
    }

    @Override
    public Object getOutputTarget() {
        return _writer.getOutputTarget();
    }
    
    @Override
    public void setSchema(FormatSchema schema)
    {
        if (schema instanceof CsvSchema) {
            if (_schema != schema) {
                _schema = (CsvSchema) schema;
                _writer = _writer.withSchema(_schema);
            }
        } else {
            super.setSchema(schema);
        }
    }

    /*
    /**********************************************************
    /* Public API, capability introspection methods
    /**********************************************************
     */

    @Override
    public boolean canUseSchema(FormatSchema schema) {
        return (schema instanceof CsvSchema);
    }
    
    @Override
    public boolean canOmitFields() {
        // Nope: CSV requires at least a placeholder
        return false;
    }
    
    /*
    /**********************************************************************
    /* Overridden methods; writing field names
    /**********************************************************************
     */
    
    /* And then methods overridden to make final, streamline some
     * aspects...
     */

    @Override
    public final void writeFieldName(String name) throws IOException, JsonGenerationException
    {
        if (_writeContext.writeFieldName(name) == JsonWriteContext.STATUS_EXPECT_VALUE) {
            _reportError("Can not write a field name, expecting a value");
        }
        _writeFieldName(name);
    }

    @Override
    public final void writeFieldName(SerializableString name)
        throws IOException, JsonGenerationException
    {
        // Object is a value, need to verify it's allowed
        if (_writeContext.writeFieldName(name.getValue()) == JsonWriteContext.STATUS_EXPECT_VALUE) {
            _reportError("Can not write a field name, expecting a value");
        }
        _writeFieldName(name.getValue());
    }

    @Override
    public final void writeStringField(String fieldName, String value)
        throws IOException, JsonGenerationException
    {
        if (_writeContext.writeFieldName(fieldName) == JsonWriteContext.STATUS_EXPECT_VALUE) {
            _reportError("Can not write a field name, expecting a value");
        }
        _writeFieldName(fieldName);
        writeString(value);
    }

    private final void _writeFieldName(String name)
        throws IOException, JsonGenerationException
    {
        // just find the matching index -- must have schema for that
        if (_schema == null) {
            _reportError("Unrecognized column '"+name+"', can not resolve without CsvSchema");
        }
        CsvSchema.Column col = _schema.column(name);
        if (col == null) {
            _reportError("Unrecognized column '"+name+"': known columns: "+_schema.getColumnDesc());
            
        }
        // and all we do is just note index to use for following value write
        _nextColumnByName = col.getIndex();
    }
    
    /*
    /**********************************************************
    /* Extended API, configuration
    /**********************************************************
     */

    public CsvGenerator enable(Feature f) {
        _csvFeatures |= f.getMask();
        _writer.setFeatures(_csvFeatures);
        return this;
    }

    public CsvGenerator disable(Feature f) {
        _csvFeatures &= ~f.getMask();
        return this;
    }

    public final boolean isEnabled(Feature f) {
        return (_csvFeatures & f.getMask()) != 0;
    }

    public CsvGenerator configure(Feature f, boolean state) {
        if (state) {
            return enable(f);
        }
        return disable(f);
    }

    /*
    /**********************************************************
    /* Public API: low-level I/O
    /**********************************************************
     */

    @Override
    public final void flush() throws IOException
    {
        _writer.flush(isEnabled(JsonGenerator.Feature.FLUSH_PASSED_TO_STREAM));
    }
    
    @Override
    public void close() throws IOException
    {
        super.close();

        // Let's mark row as closed, if we had any...
        finishRow();
        _writer.close(_ioContext.isResourceManaged() || isEnabled(JsonGenerator.Feature.AUTO_CLOSE_TARGET));
    }

    /*
    /**********************************************************
    /* Public API: structural output
    /**********************************************************
     */

    @Override
    public final void writeStartArray() throws IOException, JsonGenerationException
    {
        _verifyValueWrite("start an array");
        /* Ok to create root-level array to contain Objects/Arrays, but
         * can not nest arrays in objects
         */
        if (_writeContext.inObject()) {
            _reportError("CSV generator does not support Array values for properties");
        }
        _writeContext = _writeContext.createChildArrayContext();
        // and that's about it, really
    }

    @Override
    public final void writeEndArray() throws IOException, JsonGenerationException
    {
        if (!_writeContext.inArray()) {
            _reportError("Current context not an ARRAY but "+_writeContext.getTypeDesc());
        }
        _writeContext = _writeContext.getParent();
        // not 100% fool-proof, but chances are row should be done now
        finishRow();
    }

    @Override
    public final void writeStartObject() throws IOException, JsonGenerationException
    {
        _verifyValueWrite("start an object");
        /* No nesting for objects; can write Objects inside logical
         * root-level arrays.
         */
        if (_writeContext.inObject()) {
            _reportError("CSV generator does not support Object values for properties");
        }
        _writeContext = _writeContext.createChildObjectContext();
    }

    @Override
    public final void writeEndObject() throws IOException, JsonGenerationException
    {
        if (!_writeContext.inObject()) {
            _reportError("Current context not an object but "+_writeContext.getTypeDesc());
        }
        _writeContext = _writeContext.getParent();
        // not 100% fool-proof, but chances are row should be done now
        finishRow();
    }
    
    /*
    /**********************************************************
    /* Output method implementations, textual
    /**********************************************************
     */

    @Override
    public void writeString(String text) throws IOException,JsonGenerationException
    {
        if (text == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write String value");
        _writer.write(_columnIndex(), text);
    }

    @Override
    public void writeString(char[] text, int offset, int len) throws IOException, JsonGenerationException
    {
        _verifyValueWrite("write String value");
        _writer.write(_columnIndex(), text, offset, len);
    }

    @Override
    public final void writeString(SerializableString sstr)
        throws IOException, JsonGenerationException
    {
        _verifyValueWrite("write String value");
        _writer.write(_columnIndex(), sstr.getValue());
    }

    @Override
    public void writeRawUTF8String(byte[] text, int offset, int len)
        throws IOException, JsonGenerationException
    {
        _reportUnsupportedOperation();
    }

    @Override
    public final void writeUTF8String(byte[] text, int offset, int len)
        throws IOException, JsonGenerationException
    {
        writeString(new String(text, offset, len, "UTF-8"));
    }

    /*
    /**********************************************************
    /* Output method implementations, unprocessed ("raw")
    /**********************************************************
     */

    @Override
    public void writeRaw(String text) throws IOException, JsonGenerationException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRaw(String text, int offset, int len) throws IOException, JsonGenerationException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRaw(char[] text, int offset, int len) throws IOException, JsonGenerationException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRaw(char c) throws IOException, JsonGenerationException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRawValue(String text) throws IOException, JsonGenerationException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRawValue(String text, int offset, int len) throws IOException, JsonGenerationException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRawValue(char[] text, int offset, int len) throws IOException, JsonGenerationException {
        _reportUnsupportedOperation();
    }

    /*
    /**********************************************************
    /* Output method implementations, base64-encoded binary
    /**********************************************************
     */

    @Override
    public void writeBinary(Base64Variant b64variant, byte[] data, int offset, int len) throws IOException, JsonGenerationException
    {
        if (data == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write Binary value");
        // ok, better just Base64 encode as a String...
        if (offset > 0 || (offset+len) != data.length) {
            data = Arrays.copyOfRange(data, offset, offset+len);
        }
        String encoded = b64variant.encode(data);
        _writer.write(_columnIndex(), encoded);
    }

    /*
    /**********************************************************
    /* Output method implementations, primitive
    /**********************************************************
     */

    @Override
    public void writeBoolean(boolean state) throws IOException, JsonGenerationException
    {
        _verifyValueWrite("write boolean value");
        _writer.write(_columnIndex(), state);
    }

    @Override
    public void writeNull() throws IOException, JsonGenerationException
    {
        _verifyValueWrite("write null value");
        // !!! TODO: empty String vs String null?
        _writer.write(_columnIndex(), "");
    }

    @Override
    public void writeNumber(int i) throws IOException, JsonGenerationException
    {
        _verifyValueWrite("write number");
        _writer.write(_columnIndex(), i);
    }

    @Override
    public void writeNumber(long l) throws IOException, JsonGenerationException
    {
        // First: maybe 32 bits is enough?
        if (l <= MAX_INT_AS_LONG && l >= MIN_INT_AS_LONG) {
            writeNumber((int) l);
            return;
        }
        _verifyValueWrite("write number");
        _writer.write(_columnIndex(), l);
    }

    @Override
    public void writeNumber(BigInteger v) throws IOException, JsonGenerationException
    {
        if (v == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write number");
        _writer.write(_columnIndex(), v.toString());
    }
    
    @Override
    public void writeNumber(double d) throws IOException, JsonGenerationException
    {
        _verifyValueWrite("write number");
        _writer.write(_columnIndex(), d);
    }    

    @Override
    public void writeNumber(float f) throws IOException, JsonGenerationException
    {
        _verifyValueWrite("write number");
        _writer.write(_columnIndex(), f);
    }

    @Override
    public void writeNumber(BigDecimal dec) throws IOException, JsonGenerationException
    {
        if (dec == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write number");
        String str = isEnabled(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN) ?
                dec.toPlainString() : dec.toString();
        _writer.write(_columnIndex(), str);
    }

    @Override
    public void writeNumber(String encodedValue) throws IOException,JsonGenerationException, UnsupportedOperationException
    {
        if (encodedValue == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write number");
        _writer.write(_columnIndex(), encodedValue);
    }
    
    /*
    /**********************************************************
    /* Overrides for field methods
    /**********************************************************
     */

    @Override
    public void writeOmittedField(String fieldName)
        throws IOException, JsonGenerationException
    {
        // basically combination of "writeFieldName()" and "writeNull()"
        if (_writeContext.writeFieldName(fieldName) == JsonWriteContext.STATUS_EXPECT_VALUE) {
            _reportError("Can not skip a field, expecting a value");
        }
        // Hmmh. Should we require a match? Actually, let's use logic: if field found,
        // assumption is we must add a placeholder; if not, we can merely ignore
        CsvSchema.Column col = _schema.column(fieldName);
        if (col == null) {
            // assumed to have been removed from schema too
        } else {
            // and all we do is just note index to use for following value write
            _nextColumnByName = col.getIndex();
            // We can basically copy what 'writeNull()' does...
            _verifyValueWrite("skip positional value due to filtering");
            _writer.write(_columnIndex(), "");
        }
    }

    /*
    /**********************************************************
    /* Implementations for methods from base class
    /**********************************************************
     */
    
    @Override
    protected final void _verifyValueWrite(String typeMsg)
        throws IOException, JsonGenerationException
    {
        int status = _writeContext.writeValue();
        if (status == JsonWriteContext.STATUS_EXPECT_NAME) {
            _reportError("Can not "+typeMsg+", expecting field name");
        }
        if (_handleFirstLine) {
            _handleFirstLine();
        }
    }

    @Override
    protected void _releaseBuffers() {
        _writer._releaseBuffers();
    }

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    protected final int _columnIndex()
    {
        int ix = _nextColumnByName;
        if (ix < 0) { // if we had one, remove now
            ix = _writer.nextColumnIndex();
        }
        return ix;
    }

    /**
     * Method called when the current row is complete; typically
     * will flush possibly buffered column values, append linefeed
     * and reset state appropriately.
     */
    protected void finishRow() throws IOException, JsonGenerationException
    {
        _writer.endRow();
        _nextColumnByName = -1;
    }

    protected void _handleFirstLine() throws IOException, JsonGenerationException
    {
        _handleFirstLine = false;
        if (_schema.useHeader()) {
            int count = _schema.size();
            if (count == 0) { 
                _reportError("Schema specified that header line is to be written; but contains no column names");
            }
            for (CsvSchema.Column column : _schema) {
                _writer.writeColumnName(column.getName());
            }
            _writer.endRow();
        }
    }
}
