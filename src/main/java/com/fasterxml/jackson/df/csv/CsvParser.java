package com.fasterxml.jackson.df.csv;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.codehaus.jackson.*;
import org.codehaus.jackson.impl.JsonParserBase;
import org.codehaus.jackson.io.IOContext;
import org.codehaus.jackson.sym.BytesToNameCanonicalizer;

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
    
    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */
    
    /**
     * Codec used for data binding when (if) requested.
     */
    protected ObjectCodec _objectCodec;

    /*
    /**********************************************************
    /* Input source config, state (from ex StreamBasedParserBase)
    /**********************************************************
     */

    /**
     * Input stream that can be used for reading more content, if one
     * in use. May be null, if input comes just as a full buffer,
     * or if the stream has been closed.
     */
    protected InputStream _inputStream;

    /**
     * Current buffer from which data is read; generally data is read into
     * buffer from input source, but in some cases pre-loaded buffer
     * is handed to the parser.
     */
    protected byte[] _inputBuffer;

    /**
     * Flag that indicates whether the input buffer is recycable (and
     * needs to be returned to recycler once we are done) or not.
     *<p>
     * If it is not, it also means that parser can NOT modify underlying
     * buffer.
     */
    protected boolean _bufferRecyclable;
    
    /*
    /**********************************************************
    /* Additional parsing state
    /**********************************************************
     */

    /*
    /**********************************************************
    /* Symbol handling, decoding
    /**********************************************************
     */

    /**
     * Symbol table that contains field names encountered so far
     */
    final protected BytesToNameCanonicalizer _symbols;
    
    /**
     * Temporary buffer used for name parsing.
     */
    protected int[] _quadBuffer = NO_INTS;

    /**
     * Quads used for hash calculation
     */
    protected int _quad1, _quad2;
     
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    
    public CsvParser(IOContext ctxt, int parserFeatures, int csvFeatures,
            ObjectCodec codec,
            BytesToNameCanonicalizer sym,
            InputStream in, byte[] inputBuffer, int start, int end,
            boolean bufferRecyclable)
    {
        super(ctxt, parserFeatures);        
        _objectCodec = codec;
        _symbols = sym;

        _inputStream = in;
        _inputBuffer = inputBuffer;
        _inputPtr = start;
        _inputEnd = end;
        _bufferRecyclable = bufferRecyclable;
        
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

    /*
    /**********************************************************
    /* Former StreamBasedParserBase methods
    /**********************************************************
     */

    @Override
    public int releaseBuffered(OutputStream out) throws IOException
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
        return _inputStream;
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
        
        if (_inputStream != null) {
            int count = _inputStream.read(_inputBuffer, 0, _inputBuffer.length);
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
