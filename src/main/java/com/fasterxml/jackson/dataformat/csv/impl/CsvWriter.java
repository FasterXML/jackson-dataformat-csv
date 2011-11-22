package com.fasterxml.jackson.dataformat.csv.impl;

import java.io.IOException;
import java.io.Writer;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.io.IOContext;

/**
 * Low-level helper class that handles actual output of CSV, purely
 * based on indexes given without worrying about reordering etc.
 */
public class CsvWriter
{
    /* As an optimization we try coalescing short writes into
     * buffer; but pass longer directly.
     */
    final protected static int SHORT_WRITE = 32;

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
    
    protected final char _cfgColumnSeparator;

    protected final char _cfgQuoteCharacter;
    
    protected final char[] _cfgLineSeparator;

    protected final int _cfgLineSeparatorLength;
    
    /*
    /**********************************************************
    /* Output state
    /**********************************************************
     */

    /**
     * Index of column we expect to write next
     */
    protected int _nextColumnToWrite = -1;

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
     * Flag that indicates whether the <code>_outputBuffer</code> is recycable (and
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
    /* Construction
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
    }

    /*
    /**********************************************************
    /* Read-access to output state
    /**********************************************************
     */
    
    public Object getOutputTarget() {
        return _out;
    }

    /*
    /**********************************************************
    /* Writer API, writes
    /**********************************************************
     */

    public void write(int columnIndex, String value) throws IOException
    {
        // if not in expected order, must buffer
        if (columnIndex > _nextColumnToWrite) {
            return;
        }
        // otherwise just append...
    }

    public void endRow() throws IOException
    {
        if (_nextColumnToWrite > 0) {
            _nextColumnToWrite = 0;
            if ((_outputTail + _cfgLineSeparatorLength) > _outputEnd) {
                _flushBuffer();
            }
            System.arraycopy(_cfgLineSeparator, 0, _outputBuffer, _outputTail, _cfgLineSeparatorLength);
            _outputTail += _cfgLineSeparatorLength;
        }
    }

    /*
    /**********************************************************
    /* Output methods, unprocessed ("raw")
    /**********************************************************
     */

    public void writeRaw(String text)
        throws IOException, JsonGenerationException
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

    public void writeRaw(String text, int start, int len)
        throws IOException, JsonGenerationException
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

    public void writeRaw(char c)
        throws IOException, JsonGenerationException
    {
        if (_outputTail >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = c;
    }

    private void writeRawLong(String text)
        throws IOException, JsonGenerationException
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
