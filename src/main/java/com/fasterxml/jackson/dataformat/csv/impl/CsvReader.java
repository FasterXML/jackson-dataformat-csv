package com.fasterxml.jackson.dataformat.csv.impl;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.codehaus.jackson.*;
import org.codehaus.jackson.JsonParser.NumberType;
import org.codehaus.jackson.io.IOContext;

/**
 * Low-level helper class that handles actual reading of CSV,
 * purely based on indexes given without worrying about reordering etc.
 */
public class CsvReader
{
    
    /*
    /**********************************************************************
    /* Input handling, configuration
    /**********************************************************************
     */

    /**
     * I/O context for this reader. It handles buffer allocation
     * for the reader.
     */
    final protected IOContext _ioContext;
    
    /**
     * Input stream that can be used for reading more content, if one
     * in use. May be null, if input comes just as a full buffer,
     * or if the stream has been closed.
     */
    protected Reader _inputSource;

    /**
     * Flag that indicates whether the input buffer is recycable (and
     * needs to be returned to recycler once we are done) or not.
     *<p>
     * If it is not, it also means that parser can NOT modify underlying
     * buffer.
     */
    protected boolean _bufferRecyclable;

    protected boolean _autoCloseInput;
    
    /*
    /**********************************************************************
    /* Input handling, state
    /**********************************************************************
     */

    /**
     * Buffer that contains contents of all values after processing
     * of doubled-quotes, escaped characters.
     */
    protected final TextBuffer _textBuffer;

    /**
     * Current buffer from which data is read; generally data is read into
     * buffer from input source, but in some cases pre-loaded buffer
     * is handed to the parser.
     */
    protected char[] _inputBuffer;

    /**
     * Pointer to next available character in buffer
     */
    protected int _inputPtr = 0;

    /**
     * Index of character after last available one in the buffer.
     */
    protected int _inputEnd = 0;

    /**
     * Flag that indicates whether parser is closed or not. Gets
     * set when parser is either closed by explicit call
     * ({@link #close}) or when end-of-input is reached.
     */
    protected boolean _closed;
    
    /*
    /**********************************************************************
    /* Current input location information
    /**********************************************************************
     */

    /**
     * Number of characters/bytes that were contained in previous blocks
     * (blocks that were already processed prior to the current buffer).
     */
    protected long _currInputProcessed = 0L;

    /**
     * Current row location of current point in input buffer, starting
     * from 1, if available.
     */
    protected int _currInputRow = 1;

    /**
     * Current index of the first character of the current row in input
     * buffer. Needed to calculate column position, if necessary; benefit
     * of not having column itself is that this only has to be updated
     * once per line.
     */
    protected int _currInputRowStart = 0;

    // // // Location info at point when current token was started

    /**
     * Total number of bytes/characters read before start of current token.
     * For big (gigabyte-sized) sizes are possible, needs to be long,
     * unlike pointers and sizes related to in-memory buffers.
     */
    protected long _tokenInputTotal = 0; 

    /**
     * Input row on which current token starts, 1-based
     */
    protected int _tokenInputRow = 1;

    /**
     * Column on input row that current token starts; 0-based (although
     * in the end it'll be converted to 1-based)
     */
    protected int _tokenInputCol = 0;

    /*
    /**********************************************************************
    /* Constants and fields of former 'JsonNumericParserBase'
    /**********************************************************************
     */

    final protected static int NR_UNKNOWN = 0;

    // First, integer types

    final protected static int NR_INT = 0x0001;
    final protected static int NR_LONG = 0x0002;
    final protected static int NR_BIGINT = 0x0004;

    // And then floating point types

    final protected static int NR_DOUBLE = 0x008;
    final protected static int NR_BIGDECIMAL = 0x0010;

    // Also, we need some numeric constants

    final static BigDecimal BD_MIN_LONG = new BigDecimal(Long.MIN_VALUE);
    final static BigDecimal BD_MAX_LONG = new BigDecimal(Long.MAX_VALUE);

    final static BigDecimal BD_MIN_INT = new BigDecimal(Long.MIN_VALUE);
    final static BigDecimal BD_MAX_INT = new BigDecimal(Long.MAX_VALUE);

    final static long MIN_INT_L = (long) Integer.MIN_VALUE;
    final static long MAX_INT_L = (long) Integer.MAX_VALUE;

    // These are not very accurate, but have to do... (for bounds checks)

    final static double MIN_LONG_D = (double) Long.MIN_VALUE;
    final static double MAX_LONG_D = (double) Long.MAX_VALUE;

    final static double MIN_INT_D = (double) Integer.MIN_VALUE;
    final static double MAX_INT_D = (double) Integer.MAX_VALUE;
    
    
    // Digits, numeric
    final protected static int INT_0 = '0';
    final protected static int INT_1 = '1';
    final protected static int INT_2 = '2';
    final protected static int INT_3 = '3';
    final protected static int INT_4 = '4';
    final protected static int INT_5 = '5';
    final protected static int INT_6 = '6';
    final protected static int INT_7 = '7';
    final protected static int INT_8 = '8';
    final protected static int INT_9 = '9';

    final protected static int INT_MINUS = '-';
    final protected static int INT_PLUS = '+';
    final protected static int INT_DECIMAL_POINT = '.';

    final protected static int INT_e = 'e';
    final protected static int INT_E = 'E';

    final protected static char CHAR_NULL = '\0';
    
    // Numeric value holders: multiple fields used for
    // for efficiency

    /**
     * Bitfield that indicates which numeric representations
     * have been calculated for the current type
     */
    protected int _numTypesValid = NR_UNKNOWN;

    // First primitives

    protected int _numberInt;

    protected long _numberLong;

    protected double _numberDouble;

    // And then object types

    protected BigInteger _numberBigInt;

    protected BigDecimal _numberBigDecimal;

    // And then other information about value itself

    /**
     * Flag that indicates whether numeric value has a negative
     * value. That is, whether its textual representation starts
     * with minus character.
     */
    protected boolean _numberNegative;

    /**
     * Length of integer part of the number, in characters
     */
    protected int _intLength;

    /**
     * Length of the fractional part (not including decimal
     * point or exponent), in characters.
     * Not used for  pure integer values.
     */
    protected int _fractLength;

    /**
     * Length of the exponent part of the number, if any, not
     * including 'e' marker or sign, just digits. 
     * Not used for  pure integer values.
     */
    protected int _expLength;
    
    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    // !!!! Only to allow compilation to succeed; remove once done!
    JsonToken _currToken = null;
    
    public CsvReader(IOContext ctxt, Reader r, TextBuffer textBuffer,
            boolean autoCloseInput)
    {
        _ioContext = ctxt;
        _inputSource = r;
        _textBuffer = textBuffer;
        _autoCloseInput = autoCloseInput;
        _inputBuffer = ctxt.allocTokenBuffer();
        _bufferRecyclable = true; // since we allocated it
        _inputSource = r;
        _tokenInputRow = -1;
        _tokenInputCol = -1;
    }

    /*
    /**********************************************************************
    /* JsonParser implementations passed-through by CsvParser
    /**********************************************************************
     */
    
    public Object getInputSource() {
        return _inputSource;
    }

    public boolean isClosed() { return _closed; }
    
    public void close() throws IOException
    {
        if (!_closed) {
            _closed = true;
            try {
                _closeInput();
            } finally {
                // Also, internal buffer(s) can now be released as well
                releaseBuffers();
            }
        }
    }

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
    
    public JsonLocation getTokenLocation()
    {
        return new JsonLocation(_inputSource, getTokenCharacterOffset(),
                getTokenLineNr(), getTokenColumnNr());
    }

    public JsonLocation getCurrentLocation()
    {
        int col = _inputPtr - _currInputRowStart + 1; // 1-based
        return new JsonLocation(_inputSource,
                _currInputProcessed + _inputPtr - 1, _currInputRow, col);
    }
    
    /*
    /**********************************************************************
    /* Helper methods, input handling
    /**********************************************************************
     */

    protected final long getTokenCharacterOffset() { return _tokenInputTotal; }
    protected final int getTokenLineNr() { return _tokenInputRow; }
    protected final int getTokenColumnNr() {
        // note: value of -1 means "not available"; otherwise convert from 0-based to 1-based
        int col = _tokenInputCol;
        return (col < 0) ? col : (col + 1);
    }
    
    protected void releaseBuffers() throws IOException
    {
        _textBuffer.releaseBuffers();
        char[] buf = _inputBuffer;
        if (buf != null) {
            _inputBuffer = null;
            _ioContext.releaseTokenBuffer(buf);
        }
    }

    protected void _closeInput() throws IOException
    {
        /* 25-Nov-2008, tatus: As per [JACKSON-16] we are not to call close()
         *   on the underlying Reader, unless we "own" it, or auto-closing
         *   feature is enabled.
         *   One downside is that when using our optimized
         *   Reader (granted, we only do that for UTF-32...) this
         *   means that buffer recycling won't work correctly.
         */
        if (_inputSource != null) {
            if (_autoCloseInput || _ioContext.isResourceManaged()) {
                _inputSource.close();
            }
            _inputSource = null;
        }
    }
    
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

    /*
    /**********************************************************************
    /* Actual parsing, access methods
    /**********************************************************************
     */

    public String getText() {
        return _textBuffer.contentsAsString();
    }

    /**
     * Method called to parse the next token when we don't have any type
     * information, so that all tokens are exposed as basic String
     * values.
     * 
     * @return Column value if more found; null to indicate end of line
     */
    public String nextString() throws IOException, JsonParseException
    {
        _numTypesValid = NR_UNKNOWN;

        return null;
    }
    
    /*
    /**********************************************************************
    /* Numeric accessors for CsvParser
    /**********************************************************************
     */
    
    public Number getNumberValue() throws IOException, JsonParseException
    {
        if (_numTypesValid == NR_UNKNOWN) {
            _parseNumericValue(NR_UNKNOWN); // will also check event type
        }
        // Separate types for int types
        if (_currToken == JsonToken.VALUE_NUMBER_INT) {
            if ((_numTypesValid & NR_INT) != 0) {
                return Integer.valueOf(_numberInt);
            }
            if ((_numTypesValid & NR_LONG) != 0) {
                return Long.valueOf(_numberLong);
            }
            if ((_numTypesValid & NR_BIGINT) != 0) {
                return _numberBigInt;
            }
            // Shouldn't get this far but if we do
            return _numberBigDecimal;
        }
    
        /* And then floating point types. But here optimal type
         * needs to be big decimal, to avoid losing any data?
         */
        if ((_numTypesValid & NR_BIGDECIMAL) != 0) {
            return _numberBigDecimal;
        }
        if ((_numTypesValid & NR_DOUBLE) == 0) { // sanity check
            _throwInternal();
        }
        return Double.valueOf(_numberDouble);
    }
    
    public NumberType getNumberType() throws IOException, JsonParseException
    {
        if (_numTypesValid == NR_UNKNOWN) {
            _parseNumericValue(NR_UNKNOWN); // will also check event type
        }
        if (_currToken == JsonToken.VALUE_NUMBER_INT) {
            if ((_numTypesValid & NR_INT) != 0) {
                return NumberType.INT;
            }
            if ((_numTypesValid & NR_LONG) != 0) {
                return NumberType.LONG;
            }
            return NumberType.BIG_INTEGER;
        }
    
        /* And then floating point types. Here optimal type
         * needs to be big decimal, to avoid losing any data?
         * However... using BD is slow, so let's allow returning
         * double as type if no explicit call has been made to access
         * data as BD?
         */
        if ((_numTypesValid & NR_BIGDECIMAL) != 0) {
            return NumberType.BIG_DECIMAL;
        }
        return NumberType.DOUBLE;
    }
    
    public int getIntValue() throws IOException, JsonParseException
    {
        if ((_numTypesValid & NR_INT) == 0) {
            if (_numTypesValid == NR_UNKNOWN) { // not parsed at all
                _parseNumericValue(NR_INT); // will also check event type
            }
            if ((_numTypesValid & NR_INT) == 0) { // wasn't an int natively?
                convertNumberToInt(); // let's make it so, if possible
            }
        }
        return _numberInt;
    }
    
    public long getLongValue() throws IOException, JsonParseException
    {
        if ((_numTypesValid & NR_LONG) == 0) {
            if (_numTypesValid == NR_UNKNOWN) {
                _parseNumericValue(NR_LONG);
            }
            if ((_numTypesValid & NR_LONG) == 0) {
                convertNumberToLong();
            }
        }
        return _numberLong;
    }
    
    public BigInteger getBigIntegerValue() throws IOException, JsonParseException
    {
        if ((_numTypesValid & NR_BIGINT) == 0) {
            if (_numTypesValid == NR_UNKNOWN) {
                _parseNumericValue(NR_BIGINT);
            }
            if ((_numTypesValid & NR_BIGINT) == 0) {
                convertNumberToBigInteger();
            }
        }
        return _numberBigInt;
    }
    
    public float getFloatValue() throws IOException, JsonParseException
    {
        double value = getDoubleValue();
        /* Bounds/range checks would be tricky
         * here, so let's not bother even trying...
         */
        return (float) value;
    }
    
    public double getDoubleValue() throws IOException, JsonParseException
    {
        if ((_numTypesValid & NR_DOUBLE) == 0) {
            if (_numTypesValid == NR_UNKNOWN) {
                _parseNumericValue(NR_DOUBLE);
            }
            if ((_numTypesValid & NR_DOUBLE) == 0) {
                convertNumberToDouble();
            }
        }
        return _numberDouble;
    }
    
    public BigDecimal getDecimalValue() throws IOException, JsonParseException
    {
        if ((_numTypesValid & NR_BIGDECIMAL) == 0) {
            if (_numTypesValid == NR_UNKNOWN) {
                _parseNumericValue(NR_BIGDECIMAL);
            }
            if ((_numTypesValid & NR_BIGDECIMAL) == 0) {
                convertNumberToBigDecimal();
            }
        }
        return _numberBigDecimal;
    }

    /*
    /**********************************************************************
    /* Conversion from textual to numeric representation
    /**********************************************************************
     */
    
    /**
     * Method that will parse actual numeric value out of a syntactically
     * valid number value. Type it will parse into depends on whether
     * it is a floating point number, as well as its magnitude: smallest
     * legal type (of ones available) is used for efficiency.
     *
     * @param expType Numeric type that we will immediately need, if any;
     *   mostly necessary to optimize handling of floating point numbers
     */
    protected void _parseNumericValue(int expType)
        throws IOException, JsonParseException
    {
        // Int or float?
        if (_currToken == JsonToken.VALUE_NUMBER_INT) {
            char[] buf = _textBuffer.getTextBuffer();
            int offset = _textBuffer.getTextOffset();
            int len = _intLength;
            if (_numberNegative) {
                ++offset;
            }
            if (len <= 9) { // definitely fits in int
                int i = NumberInput.parseInt(buf, offset, len);
                _numberInt = _numberNegative ? -i : i;
                _numTypesValid = NR_INT;
                return;
            }
            if (len <= 18) { // definitely fits AND is easy to parse using 2 int parse calls
                long l = NumberInput.parseLong(buf, offset, len);
                if (_numberNegative) {
                    l = -l;
                }
                // [JACKSON-230] Could still fit in int, need to check
                if (len == 10) {
                    if (_numberNegative) {
                        if (l >= MIN_INT_L) {
                            _numberInt = (int) l;
                            _numTypesValid = NR_INT;
                            return;
                        }
                    } else {
                        if (l <= MAX_INT_L) {
                            _numberInt = (int) l;
                            _numTypesValid = NR_INT;
                            return;
                        }
                    }
                }
                _numberLong = l;
                _numTypesValid = NR_LONG;
                return;
            }
            _parseSlowIntValue(expType, buf, offset, len);
            return;
        }
        if (_currToken == JsonToken.VALUE_NUMBER_FLOAT) {
            _parseSlowFloatValue(expType);
            return;
        }
        _reportError("Current token ("+_currToken+") not numeric, can not use numeric value accessors");
    }
    
    private final void _parseSlowFloatValue(int expType)
        throws IOException, JsonParseException
    {
        /* Nope: floating point. Here we need to be careful to get
         * optimal parsing strategy: choice is between accurate but
         * slow (BigDecimal) and lossy but fast (Double). For now
         * let's only use BD when explicitly requested -- it can
         * still be constructed correctly at any point since we do
         * retain textual representation
         */
        try {
            if (expType == NR_BIGDECIMAL) {
                _numberBigDecimal = _textBuffer.contentsAsDecimal();
                _numTypesValid = NR_BIGDECIMAL;
            } else {
                // Otherwise double has to do
                _numberDouble = _textBuffer.contentsAsDouble();
                _numTypesValid = NR_DOUBLE;
            }
        } catch (NumberFormatException nex) {
            // Can this ever occur? Due to overflow, maybe?
            throw constructError("Malformed numeric value '"+_textBuffer.contentsAsString()+"'", nex);
        }
    }
    
    private final void _parseSlowIntValue(int expType, char[] buf, int offset, int len)
        throws IOException, JsonParseException
    {
        String numStr = _textBuffer.contentsAsString();
        try {
            // [JACKSON-230] Some long cases still...
            if (NumberInput.inLongRange(buf, offset, len, _numberNegative)) {
                // Probably faster to construct a String, call parse, than to use BigInteger
                _numberLong = Long.parseLong(numStr);
                _numTypesValid = NR_LONG;
            } else {
                // nope, need the heavy guns... (rare case)
                _numberBigInt = new BigInteger(numStr);
                _numTypesValid = NR_BIGINT;
            }
        } catch (NumberFormatException nex) {
            // Can this ever occur? Due to overflow, maybe?
            throw constructError("Malformed numeric value '"+numStr+"'", nex);
        }
    }

    /*
    /**********************************************************************
    /* Numeric conversions
    /**********************************************************************
     */    
    
    protected void convertNumberToInt()
        throws IOException, JsonParseException
    {
        // First, converting from long ought to be easy
        if ((_numTypesValid & NR_LONG) != 0) {
            // Let's verify it's lossless conversion by simple roundtrip
            int result = (int) _numberLong;
            if (((long) result) != _numberLong) {
                _reportError("Numeric value ("+getText()+") out of range of int");
            }
            _numberInt = result;
        } else if ((_numTypesValid & NR_BIGINT) != 0) {
            // !!! Should check for range...
            _numberInt = _numberBigInt.intValue();
        } else if ((_numTypesValid & NR_DOUBLE) != 0) {
            // Need to check boundaries
            if (_numberDouble < MIN_INT_D || _numberDouble > MAX_INT_D) {
                reportOverflowInt();
            }
            _numberInt = (int) _numberDouble;
        } else if ((_numTypesValid & NR_BIGDECIMAL) != 0) {
            if (BD_MIN_INT.compareTo(_numberBigDecimal) > 0 
                || BD_MAX_INT.compareTo(_numberBigDecimal) < 0) {
                reportOverflowInt();
            }
            _numberInt = _numberBigDecimal.intValue();
        } else {
            _throwInternal(); // should never get here
        }
    
        _numTypesValid |= NR_INT;
    }
    
    protected void convertNumberToLong()
        throws IOException, JsonParseException
    {
        if ((_numTypesValid & NR_INT) != 0) {
            _numberLong = (long) _numberInt;
        } else if ((_numTypesValid & NR_BIGINT) != 0) {
            // !!! Should check for range...
            _numberLong = _numberBigInt.longValue();
        } else if ((_numTypesValid & NR_DOUBLE) != 0) {
            // Need to check boundaries
            if (_numberDouble < MIN_LONG_D || _numberDouble > MAX_LONG_D) {
                reportOverflowLong();
            }
            _numberLong = (long) _numberDouble;
        } else if ((_numTypesValid & NR_BIGDECIMAL) != 0) {
            if (BD_MIN_LONG.compareTo(_numberBigDecimal) > 0 
                || BD_MAX_LONG.compareTo(_numberBigDecimal) < 0) {
                reportOverflowLong();
            }
            _numberLong = _numberBigDecimal.longValue();
        } else {
            _throwInternal(); // should never get here
        }
    
        _numTypesValid |= NR_LONG;
    }
    
    protected void convertNumberToBigInteger()
        throws IOException, JsonParseException
    {
        if ((_numTypesValid & NR_BIGDECIMAL) != 0) {
            // here it'll just get truncated, no exceptions thrown
            _numberBigInt = _numberBigDecimal.toBigInteger();
        } else if ((_numTypesValid & NR_LONG) != 0) {
            _numberBigInt = BigInteger.valueOf(_numberLong);
        } else if ((_numTypesValid & NR_INT) != 0) {
            _numberBigInt = BigInteger.valueOf(_numberInt);
        } else if ((_numTypesValid & NR_DOUBLE) != 0) {
            _numberBigInt = BigDecimal.valueOf(_numberDouble).toBigInteger();
        } else {
            _throwInternal(); // should never get here
        }
        _numTypesValid |= NR_BIGINT;
    }
    
    protected void convertNumberToDouble()
        throws IOException, JsonParseException
    {
        /* 05-Aug-2008, tatus: Important note: this MUST start with
         *   more accurate representations, since we don't know which
         *   value is the original one (others get generated when
         *   requested)
         */
    
        if ((_numTypesValid & NR_BIGDECIMAL) != 0) {
            _numberDouble = _numberBigDecimal.doubleValue();
        } else if ((_numTypesValid & NR_BIGINT) != 0) {
            _numberDouble = _numberBigInt.doubleValue();
        } else if ((_numTypesValid & NR_LONG) != 0) {
            _numberDouble = (double) _numberLong;
        } else if ((_numTypesValid & NR_INT) != 0) {
            _numberDouble = (double) _numberInt;
        } else {
            _throwInternal(); // should never get here
        }
    
        _numTypesValid |= NR_DOUBLE;
    }
    
    protected void convertNumberToBigDecimal()
        throws IOException, JsonParseException
    {
        /* 05-Aug-2008, tatus: Important note: this MUST start with
         *   more accurate representations, since we don't know which
         *   value is the original one (others get generated when
         *   requested)
         */
    
        if ((_numTypesValid & NR_DOUBLE) != 0) {
            /* Let's actually parse from String representation,
             * to avoid rounding errors that non-decimal floating operations
             * would incur
             */
            _numberBigDecimal = new BigDecimal(getText());
        } else if ((_numTypesValid & NR_BIGINT) != 0) {
            _numberBigDecimal = new BigDecimal(_numberBigInt);
        } else if ((_numTypesValid & NR_LONG) != 0) {
            _numberBigDecimal = BigDecimal.valueOf(_numberLong);
        } else if ((_numTypesValid & NR_INT) != 0) {
            _numberBigDecimal = BigDecimal.valueOf((long) _numberInt);
        } else {
            _throwInternal(); // should never get here
        }
        _numTypesValid |= NR_BIGDECIMAL;
    }

    /*
    /**********************************************************
    /* Number handling exceptions
    /**********************************************************
     */    
    
    protected void reportUnexpectedNumberChar(int ch, String comment)
        throws JsonParseException
    {
        String msg = "Unexpected character ("+_getCharDesc(ch)+") in numeric value";
        if (comment != null) {
            msg += ": "+comment;
        }
        _reportError(msg);
    }
    
    protected void reportInvalidNumber(String msg)
        throws JsonParseException
    {
        _reportError("Invalid numeric value: "+msg);
    }
    
    protected void reportOverflowInt()
        throws IOException, JsonParseException
    {
        _reportError("Numeric value ("+getText()+") out of range of int ("+Integer.MIN_VALUE+" - "+Integer.MAX_VALUE+")");
    }
    
    protected void reportOverflowLong()
        throws IOException, JsonParseException
    {
        _reportError("Numeric value ("+getText()+") out of range of long ("+Long.MIN_VALUE+" - "+Long.MAX_VALUE+")");
    }

    protected final JsonParseException constructError(String msg, Throwable t)
    {
        return new JsonParseException(msg, getCurrentLocation(), t);
    }
    
    protected final static String _getCharDesc(int ch)
    {
        char c = (char) ch;
        if (Character.isISOControl(c)) {
            return "(CTRL-CHAR, code "+ch+")";
        }
        if (ch > 255) {
            return "'"+c+"' (code "+ch+" / 0x"+Integer.toHexString(ch)+")";
        }
        return "'"+c+"' (code "+ch+")";
    }
    
    private void _throwInternal() {
        throw new IllegalStateException("Internal error: code path should never get executed");
    }

    protected final void _reportError(String msg)
        throws JsonParseException
    {
        throw new JsonParseException(msg, getCurrentLocation());
    }
}
