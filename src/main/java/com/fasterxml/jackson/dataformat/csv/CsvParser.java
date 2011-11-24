package com.fasterxml.jackson.dataformat.csv;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.codehaus.jackson.*;
import org.codehaus.jackson.impl.JsonParserMinimalBase;
import org.codehaus.jackson.impl.JsonReadContext;
import org.codehaus.jackson.io.IOContext;
import org.codehaus.jackson.util.BufferRecycler;

import com.fasterxml.jackson.dataformat.csv.impl.CsvReader;
import com.fasterxml.jackson.dataformat.csv.impl.TextBuffer;

public class CsvParser
    extends JsonParserMinimalBase
{
    /**
     * Enumeration that defines all togglable features for Smile generators.
     */
    public enum Feature {
        BOGUS(true) // placeholder
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
        
        public boolean enabledByDefault() { return _defaultState; }
        public int getMask() { return _mask; }
    }

    private final static CsvSchema EMPTY_SCHEMA;
    static {
        EMPTY_SCHEMA = CsvSchema.emptySchema();
    }
    
    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */
    
    /**
     * Codec used for data binding when (if) requested.
     */
    protected ObjectCodec _objectCodec;

    /**
     * Definition of columns being read. Initialized to "empty" instance, which
     * has default configuration settings.
     */
    protected CsvSchema _schema = EMPTY_SCHEMA;
    
    protected final CsvReader _reader;

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
     * Buffer that contains contents of all values after processing
     * of doubled-quotes, escaped characters.
     */
    protected final TextBuffer _textBuffer;
    
    /**
     * We will hold on to decoded binary data, for duration of
     * current event, so that multiple calls to
     * {@link #getBinaryValue} will not need to decode data more
     * than once.
     */
    protected byte[] _binaryValue;
    
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
        _parsingContext = JsonReadContext.createRootContext();
        _reader = new CsvReader(ctxt, reader, _schema, _textBuffer,
                isEnabled(JsonParser.Feature.AUTO_CLOSE_SOURCE));
    }

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
        return (schema instanceof FormatSchema);
    }

    @Override
    public void setSchema(FormatSchema schema)
    {
        if (schema instanceof CsvSchema) {
            _schema = (CsvSchema) schema;
            _reader.setSchema(_schema);
        } else {
            super.setSchema(schema);
        }
    }

    @Override
    public int releaseBuffered(Writer out) throws IOException
    {
        return _reader.releaseBuffered(out);
    }

    @Override
    public boolean isClosed() { return _reader.isClosed(); }

    @Override
    public void close() throws IOException { _reader.close(); }
    
    /*
    /**********************************************************
    /* Location info
    /**********************************************************
     */
    
    @Override
    public JsonStreamContext getParsingContext() {
        // TODO Auto-generated method stub
        return null;
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

    @Override
    public JsonToken nextToken() throws IOException, JsonParseException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getCurrentName() throws IOException, JsonParseException {
        // TODO Auto-generated method stub
        return null;
    }
    
    /*
    /**********************************************************
    /* String value handling
    /**********************************************************
     */

    // For now we do not store char[] representation...
    @Override
    public boolean hasTextCharacters() {
        return false;
    }
    
    @Override
    public String getText() throws IOException, JsonParseException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public char[] getTextCharacters() throws IOException, JsonParseException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getTextLength() throws IOException, JsonParseException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getTextOffset() throws IOException, JsonParseException {
        // TODO Auto-generated method stub
        return 0;
    }
    
    /*
    /**********************************************************************
    /* Binary (base64)
    /**********************************************************************
     */

    @Override
    public byte[] getBinaryValue(Base64Variant variant) throws IOException,
            JsonParseException {
        // TODO Auto-generated method stub
        return null;
    }

    /*
    /**********************************************************************
    /* Number accessors
    /**********************************************************************
     */

    @Override
    public NumberType getNumberType() throws IOException, JsonParseException {
        return _reader.getNumberType();
    }
    
    @Override
    public Number getNumberValue() throws IOException, JsonParseException {
        return _reader.getNumberValue();
    }

    @Override
    public int getIntValue() throws IOException, JsonParseException {
        return _reader.getIntValue();
    }
    
    @Override
    public long getLongValue() throws IOException, JsonParseException {
        return _reader.getLongValue();
    }
    
    @Override
    public BigInteger getBigIntegerValue() throws IOException, JsonParseException {
        return _reader.getBigIntegerValue();
    }
    
    @Override
    public float getFloatValue() throws IOException, JsonParseException {
        return _reader.getFloatValue();
    }
    
    @Override
    public double getDoubleValue() throws IOException, JsonParseException {
        return _reader.getDoubleValue();
    }
    
    @Override
    public BigDecimal getDecimalValue() throws IOException, JsonParseException {
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
    /* Internal methods
    /**********************************************************************
     */
}
