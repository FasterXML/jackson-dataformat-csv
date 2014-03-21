package com.fasterxml.jackson.dataformat.csv.impl;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

/**
 * Low-level helper class that handles actual output of CSV, purely
 * based on indexes given without worrying about reordering etc.
 */
public final class CsvWriter
{
    /* As an optimization we try coalescing short writes into
     * buffer; but pass longer directly.
     */
    final protected static int SHORT_WRITE = 32;

    /* Also: only do check for optional quotes for short
     * values; longer ones will always be quoted.
     */
    final protected static int MAX_QUOTE_CHECK = 20;
    
    final protected BufferedValue[] NO_BUFFERED = new BufferedValue[0];

    private final static char[] TRUE_CHARS = "true".toCharArray();
    private final static char[] FALSE_CHARS = "false".toCharArray();
    
    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    final protected IOContext _ioContext;
    
    /**
     * Underlying {@link Writer} used for output.
     */
    final protected Writer _out;
    
    final protected char _cfgColumnSeparator;

    final protected char _cfgQuoteCharacter;
    
    final protected char[] _cfgLineSeparator;

    final protected int _cfgLineSeparatorLength;

    /**
     * Lowest-valued character that is safe to output without using
     * quotes around value
     */
    final protected int _cfgMinSafeChar;
    
    /*
    /**********************************************************
    /* Output state
    /**********************************************************
     */

    /**
     * Index of column we expect to write next
     */
    protected int _nextColumnToWrite = 0;

    /**
     * And if output comes in shuffled order we will need to do 
     * bit of ordering.
     */
    protected BufferedValue[] _buffered = NO_BUFFERED;

    /**
     * Index of the last buffered value
     */
    protected int _lastBuffered = -1;
    
    /*
    /**********************************************************
    /* Output buffering, low-level
    /**********************************************************
     */
    
    /**
     * Intermediate buffer in which contents are buffered before
     * being written using {@link #_out}.
     */
    protected char[] _outputBuffer;

    /**
     * Flag that indicates whether the <code>_outputBuffer</code> is recyclable (and
     * needs to be returned to recycler once we are done) or not.
     */
    protected boolean _bufferRecyclable;
    
    /**
     * Pointer to the next available byte in {@link #_outputBuffer}
     */
    protected int _outputTail = 0;

    /**
     * Offset to index after the last valid index in {@link #_outputBuffer}.
     * Typically same as length of the buffer.
     */
    protected final int _outputEnd;
    
    /**
     * Let's keep track of how many bytes have been output, may prove useful
     * when debugging. This does <b>not</b> include bytes buffered in
     * the output buffer, just bytes that have been written using underlying
     * stream writer.
     */
    protected int _charsWritten;
    
    /*
    /**********************************************************
    /* Construction, (re)configuration
    /**********************************************************
     */

    public CsvWriter(IOContext ctxt, Writer out,
            char columnSeparator, char quoteChar, char[] linefeed)
    {
        _ioContext = ctxt;
        _outputBuffer = ctxt.allocConcatBuffer();
        _bufferRecyclable = true;
        _outputEnd = _outputBuffer.length;
        _out = out;

        _cfgColumnSeparator = columnSeparator;
        _cfgQuoteCharacter = quoteChar;
        _cfgLineSeparator = linefeed;
        _cfgLineSeparatorLength = linefeed.length;

        _cfgMinSafeChar = _calcSafeChar();
    }

    public CsvWriter(CsvWriter base, CsvSchema newSchema)
    {
        _ioContext = base._ioContext;
        _outputBuffer = base._outputBuffer;
        _bufferRecyclable = base._bufferRecyclable;
        _outputEnd = base._outputEnd;
        _out = base._out;

        _cfgColumnSeparator = newSchema.getColumnSeparator();
        _cfgQuoteCharacter = newSchema.getQuoteChar();
        _cfgLineSeparator = newSchema.getLineSeparator();
        _cfgLineSeparatorLength = _cfgLineSeparator.length;
        _cfgMinSafeChar = _calcSafeChar();
    }  
    
    private final int _calcSafeChar()
    {
        int min = Math.max(_cfgColumnSeparator, _cfgQuoteCharacter);
        for (int i = 0; i < _cfgLineSeparatorLength; ++i) {
            min = Math.max(min, _cfgLineSeparator[i]);
        }
        return min+1;
    }

    public CsvWriter withSchema(CsvSchema schema) {
        return new CsvWriter(this, schema);
    }

    /*
    /**********************************************************
    /* Read-access to output state
    /**********************************************************
     */
    
    public Object getOutputTarget() {
        return _out;
    }

    public int nextColumnIndex() {
        return _nextColumnToWrite;
    }
    
    /*
    /**********************************************************
    /* Writer API, writes from generator
    /**********************************************************
     */

    public void write(int columnIndex, String value) throws IOException
    {
        // easy case: all in order
        if (columnIndex == _nextColumnToWrite) {
            appendValue(value);
            ++_nextColumnToWrite;
            return;
        }
        _buffer(columnIndex, BufferedValue.buffered(value));
    }

    public void write(int columnIndex, char[] ch, int offset, int len) throws IOException
    {
        // !!! TODO: optimize
        write(columnIndex, new String(ch, offset, len));
    }
    
    public void write(int columnIndex, int value) throws IOException
    {
        // easy case: all in order
        if (columnIndex == _nextColumnToWrite) {
            appendValue(value);
            ++_nextColumnToWrite;
            return;
        }
        _buffer(columnIndex, BufferedValue.buffered(value));
    }

    public void write(int columnIndex, long value) throws IOException
    {
        // easy case: all in order
        if (columnIndex == _nextColumnToWrite) {
            appendValue(value);
            ++_nextColumnToWrite;
            return;
        }
        _buffer(columnIndex, BufferedValue.buffered(value));
    }

    public void write(int columnIndex, float value) throws IOException
    {
        // easy case: all in order
        if (columnIndex == _nextColumnToWrite) {
            appendValue(value);
            ++_nextColumnToWrite;
            return;
        }
        _buffer(columnIndex, BufferedValue.buffered(value));
    }

    public void write(int columnIndex, double value) throws IOException
    {
        // easy case: all in order
        if (columnIndex == _nextColumnToWrite) {
            appendValue(value);
            ++_nextColumnToWrite;
            return;
        }
        _buffer(columnIndex, BufferedValue.buffered(value));
    }


    public void write(int columnIndex, boolean value) throws IOException
    {
        // easy case: all in order
        if (columnIndex == _nextColumnToWrite) {
            appendValue(value);
            ++_nextColumnToWrite;
            return;
        }
        _buffer(columnIndex, BufferedValue.buffered(value));
    }
    
    public void writeColumnName(String name) throws IOException
    {
        appendValue(name);
        ++_nextColumnToWrite;
    }

    public void endRow() throws IOException
    {
        // First things first; any buffered?
        if (_lastBuffered >= 0) {
            final int last = _lastBuffered;
            _lastBuffered = -1;
            for (int i = _nextColumnToWrite; i <= last; ++i) {
                BufferedValue value = _buffered[i];
                if (i > _nextColumnToWrite) {
                    appendColumnSeparator();
                }
                if (value != null) {
                    _buffered[i] = null;
                    value.write(this);
                }
            }
        } else if (_nextColumnToWrite <= 0) { // empty line; do nothing
            return;
        }
        // write line separator
        _nextColumnToWrite = 0;
        if ((_outputTail + _cfgLineSeparatorLength) > _outputEnd) {
            _flushBuffer();
        }
        System.arraycopy(_cfgLineSeparator, 0, _outputBuffer, _outputTail, _cfgLineSeparatorLength);
        _outputTail += _cfgLineSeparatorLength;
    }
    
    /*
    /**********************************************************
    /* Writer API, writes via buffered values
    /**********************************************************
     */

    protected void appendValue(String value) throws IOException
    {
        if (_outputTail >= _outputEnd) {
            _flushBuffer();
        }
        if (_nextColumnToWrite > 0) {
            appendColumnSeparator();
        }
        /* First: determine if we need quotes; simple heuristics;
         * only check for short Strings, stop if something found
         */
        final int len = value.length();
        if (_mayNeedQuotes(value, len)) {
            _writeQuoted(value);
        } else {
            writeRaw(value);
        }
        
    }
    
    protected void appendValue(int value) throws IOException
    {
        // up to 10 digits and possible minus sign, leading comma
        if ((_outputTail + 12) > _outputTail) {
            _flushBuffer();
        }
        if (_nextColumnToWrite > 0) {
            _outputBuffer[_outputTail++] = _cfgColumnSeparator;
        }
        _outputTail = NumberOutput.outputInt(value, _outputBuffer, _outputTail);
    }

    protected void appendValue(long value) throws IOException
    {
        // up to 20 digits, minus sign, leading comma
        if ((_outputTail + 22) > _outputTail) {
            _flushBuffer();
        }
        if (_nextColumnToWrite > 0) {
            _outputBuffer[_outputTail++] = _cfgColumnSeparator;
        }
        _outputTail = NumberOutput.outputLong(value, _outputBuffer, _outputTail);
    }

    protected void appendValue(float value) throws IOException
    {
        String str = NumberOutput.toString(value);
        final int len = str.length();
        if ((_outputTail + len) >= _outputTail) { // >= to include possible comma too
            _flushBuffer();
        }
        if (_nextColumnToWrite > 0) {
            _outputBuffer[_outputTail++] = _cfgColumnSeparator;
        }
        writeRaw(str);
    }

    protected void appendValue(double value) throws IOException
    {
        String str = NumberOutput.toString(value);
        final int len = str.length();
        if ((_outputTail + len) >= _outputTail) { // >= to include possible comma too
            _flushBuffer();
        }
        if (_nextColumnToWrite > 0) {
            _outputBuffer[_outputTail++] = _cfgColumnSeparator;
        }
        writeRaw(str);
    }

    protected void appendValue(boolean value) throws IOException
    {
        char[] ch = value ? TRUE_CHARS : FALSE_CHARS;
        final int len = ch.length;
        if ((_outputTail + len) >= _outputTail) { // >= to include possible comma too
            _flushBuffer();
        }
        if (_nextColumnToWrite > 0) {
            _outputBuffer[_outputTail++] = _cfgColumnSeparator;
        }
        System.arraycopy(ch, 0, _outputBuffer, _outputTail, len);
        _outputTail += len;
    }

    protected void appendColumnSeparator() throws IOException {
        if (_outputTail >= _outputTail) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = _cfgColumnSeparator;
    }
    
    /*
    /**********************************************************
    /* Output methods, unprocessed ("raw")
    /**********************************************************
     */

    public void _writeQuoted(String text) throws IOException
    {
        if (_outputTail >= _outputEnd) {
            _flushBuffer();
        }
        final char q = _cfgQuoteCharacter;
        _outputBuffer[_outputTail++] = q;
        // simple case: if we have enough room, no need for boundary checks
        final int len = text.length();
        if ((_outputTail + len + len) >= _outputEnd) {
            _writeLongQuoted(text);
            return;
        }
        for (int i = 0; i < len; ++i) {
            char c = text.charAt(i);
            if (c == q) { // double up
                _outputBuffer[_outputTail++] = _cfgQuoteCharacter;
                if (_outputTail >= _outputEnd) {
                    _flushBuffer();
                }
            }
            _outputBuffer[_outputTail++] = c;
        }
        _outputBuffer[_outputTail++] = q;
    }
    
    private final void _writeLongQuoted(String text) throws IOException
    {
        final int len = text.length();
        for (int i = 0; i < len; ++i) {
            if (_outputTail >= _outputEnd) {
                _flushBuffer();
            }
            char c = text.charAt(i);
            if (c == _cfgQuoteCharacter) { // double up
                _outputBuffer[_outputTail++] = _cfgQuoteCharacter;
                if (_outputTail >= _outputEnd) {
                    _flushBuffer();
                }
            }
            _outputBuffer[_outputTail++] = c;
        }
        if (_outputTail >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = _cfgQuoteCharacter;
    }
    
    public void writeRaw(String text) throws IOException
    {
        // Nothing to check, can just output as is
        int len = text.length();
        int room = _outputEnd - _outputTail;

        if (room == 0) {
            _flushBuffer();
            room = _outputEnd - _outputTail;
        }
        // But would it nicely fit in? If yes, it's easy
        if (room >= len) {
            text.getChars(0, len, _outputBuffer, _outputTail);
            _outputTail += len;
        } else {
            writeRawLong(text);
        }
    }

    public void writeRaw(String text, int start, int len) throws IOException
    {
        // Nothing to check, can just output as is
        int room = _outputEnd - _outputTail;

        if (room < len) {
            _flushBuffer();
            room = _outputEnd - _outputTail;
        }
        // But would it nicely fit in? If yes, it's easy
        if (room >= len) {
            text.getChars(start, start+len, _outputBuffer, _outputTail);
            _outputTail += len;
        } else {                
            writeRawLong(text.substring(start, start+len));
        }
    }

    public void writeRaw(char[] text, int offset, int len)
        throws IOException, JsonGenerationException
    {
        // Only worth buffering if it's a short write?
        if (len < SHORT_WRITE) {
            int room = _outputEnd - _outputTail;
            if (len > room) {
                _flushBuffer();
            }
            System.arraycopy(text, offset, _outputBuffer, _outputTail, len);
            _outputTail += len;
            return;
        }
        // Otherwise, better just pass through:
        _flushBuffer();
        _out.write(text, offset, len);
    }

    public void writeRaw(char c) throws IOException
    {
        if (_outputTail >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = c;
    }

    private void writeRawLong(String text) throws IOException
    {
        int room = _outputEnd - _outputTail;
        // If not, need to do it by looping
        text.getChars(0, room, _outputBuffer, _outputTail);
        _outputTail += room;
        _flushBuffer();
        int offset = room;
        int len = text.length() - room;

        while (len > _outputEnd) {
            int amount = _outputEnd;
            text.getChars(offset, offset+amount, _outputBuffer, 0);
            _outputTail = amount;
            _flushBuffer();
            offset += amount;
            len -= amount;
        }
        // And last piece (at most length of buffer)
        text.getChars(offset, offset+len, _outputBuffer, 0);
        _outputTail = len;
    }
    
    /*
    /**********************************************************
    /* Writer API, state changes
    /**********************************************************
     */
    
    public void flush(boolean flushStream) throws IOException
    {
        _flushBuffer();
        if (flushStream) {
            _out.flush();
        }
    }

    public void close(boolean autoClose) throws IOException
    {
        _flushBuffer();
        if (autoClose) {
            _out.close();
        } else {
            // If we can't close it, we should at least flush
            _out.flush();
        }
        // Internal buffer(s) generator has can now be released as well
        _releaseBuffers();
    }
    
    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    /**
     * Helper method that determines whether given String is likely
     * to require quoting; check tries to optimize for speed.
     */
    protected final boolean _mayNeedQuotes(String value, int length)
    {
        // let's not bother checking long Strings, just quote already:
        if (length > MAX_QUOTE_CHECK) {
            return true;
        }
        for (int i = 0; i < length; ++i) {
            if (value.charAt(i) < _cfgMinSafeChar) {
                return true;
            }
        }
        return false;
    }
    
    protected void _buffer(int index, BufferedValue v)
    {
        _lastBuffered = Math.max(_lastBuffered, index);
        if (index >= _buffered.length) {
            _buffered = Arrays.copyOf(_buffered, index+4);
        }
        _buffered[index] = v;
    }

    protected final void _flushBuffer() throws IOException
    {
        if (_outputTail > 0) {
            _charsWritten += _outputTail;
            _out.write(_outputBuffer, 0, _outputTail);
            _outputTail = 0;
        }
    }

    public void _releaseBuffers()
    {
        char[] buf = _outputBuffer;
        if (buf != null && _bufferRecyclable) {
            _outputBuffer = null;
            _ioContext.releaseConcatBuffer(buf);
        }
    }
}
