package com.fasterxml.jackson.dataformat.csv;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.codehaus.jackson.*;
import org.codehaus.jackson.impl.JsonParserBase;
import org.codehaus.jackson.io.IOContext;

public class CsvParser
    extends JsonParserBase
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

    private final static int[] NO_INTS = new int[0];

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
    
    /*
    /**********************************************************************
    /* Input source config, state (from ex StreamBasedParserBase)
    /**********************************************************************
     */

    /**
     * Input stream that can be used for reading more content, if one
     * in use. May be null, if input comes just as a full buffer,
     * or if the stream has been closed.
     */
    protected Reader _inputSource;

    /**
     * Current buffer from which data is read; generally data is read into
     * buffer from input source, but in some cases pre-loaded buffer
     * is handed to the parser.
     */
    protected char[] _inputBuffer;

    /**
     * Flag that indicates whether the input buffer is recycable (and
     * needs to be returned to recycler once we are done) or not.
     *<p>
     * If it is not, it also means that parser can NOT modify underlying
     * buffer.
     */
    protected boolean _bufferRecyclable;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */
    
    public CsvParser(IOContext ctxt, int parserFeatures, int csvFeatures,
            ObjectCodec codec, Reader reader)
    {
        super(ctxt, parserFeatures);        
        _objectCodec = codec;
        _inputSource = reader;
        _tokenInputRow = -1;
        _tokenInputCol = -1;
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
        } else {
            super.setSchema(schema);
        }
    }
    
    /*
    /**********************************************************
    /* Former StreamBasedParserBase methods
    /**********************************************************
     */

    @Override
    public int releaseBuffered(Writer out) throws IOException
    {
        int count = _inputEnd - _inputPtr;
        if (count < 1) {
            return 0;
        }
        // let's just advance ptr to end
        int origPtr = _inputPtr;
        out.write(_inputBuffer, origPtr, count);
        return count;
    }

    @Override
    public Object getInputSource() {
        return _inputSource;
    }
    
    /*
    /**********************************************************
    /* Low-level reading, other
    /**********************************************************
     */
    
    @Override
    protected final boolean loadMore()
        throws IOException
    {
        _currInputProcessed += _inputEnd;
        _currInputRowStart -= _inputEnd;
        
        if (_inputSource != null) {
            int count = _inputSource.read(_inputBuffer, 0, _inputBuffer.length);
            if (count > 0) {
                _inputPtr = 0;
                _inputEnd = count;
                return true;
            }
            // End of input
            _closeInput();
            // Should never return 0, so let's fail
            if (count == 0) {
                throw new IOException("InputStream.read() returned 0 characters when trying to read "+_inputBuffer.length+" bytes");
            }
        }
        return false;
    }

    @Override
    protected void _closeInput() throws IOException {
        // TODO Auto-generated method stub
        
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
    
    /*
    /**********************************************************
    /* String handling
    /**********************************************************
     */

    @Override
    public String getText() throws IOException, JsonParseException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void _finishString() throws IOException, JsonParseException {
        // TODO Auto-generated method stub
        
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
    /**********************************************************
    /* Binary (base64)
    /**********************************************************
     */

    @Override
    protected byte[] _decodeBase64(Base64Variant arg0) throws IOException,
            JsonParseException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public byte[] getBinaryValue(Base64Variant arg0) throws IOException,
            JsonParseException {
        // TODO Auto-generated method stub
        return null;
    }

    /*
    /**********************************************************
    /* Numeric access
    /**********************************************************
     */

    @Override
    public BigInteger getBigIntegerValue() throws IOException,
            JsonParseException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BigDecimal getDecimalValue() throws IOException, JsonParseException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double getDoubleValue() throws IOException, JsonParseException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public float getFloatValue() throws IOException, JsonParseException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getIntValue() throws IOException, JsonParseException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getLongValue() throws IOException, JsonParseException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public NumberType getNumberType() throws IOException, JsonParseException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Number getNumberValue() throws IOException, JsonParseException {
        // TODO Auto-generated method stub
        return null;
    }
}
