package com.fasterxml.jackson.dataformat.csv.impl;

import java.io.IOException;

/**
 * Helper class used for holding values for a while until output
 * can proceed in expected order.
 */
public abstract class BufferedValue
{
    private BufferedValue() { }
    
    public abstract void write(CsvWriter w) throws IOException;
    
    public static BufferedValue buffered(String v) { return new TextValue(v); }
    public static BufferedValue buffered(int v) { return new IntValue(v); }
    public static BufferedValue buffered(long v) { return new LongValue(v); }
    public static BufferedValue buffered(double v) { return new DoubleValue(v); }
    public static BufferedValue buffered(boolean v) { return new BooleanValue(v); }
    
    protected final static class TextValue extends BufferedValue
    {
        private final String _value;
        
        public TextValue(String v) { _value = v; }

        public void write(CsvWriter w) throws IOException {
            w.appendValue(_value);
        }
    }

    protected final static class IntValue extends BufferedValue
    {
        private final int _value;
        
        public IntValue(int v) { _value = v; }

        public void write(CsvWriter w) throws IOException {
            w.appendValue(_value);
        }
    }

    protected final static class LongValue extends BufferedValue
    {
        private final long _value;
        
        public LongValue(long v) { _value = v; }

        public void write(CsvWriter w) throws IOException {
            w.appendValue(_value);
        }
    }

    protected final static class DoubleValue extends BufferedValue
    {
        private final double _value;
        
        public DoubleValue(double v) { _value = v; }

        public void write(CsvWriter w) throws IOException {
            w.appendValue(_value);
        }
    }

    protected final static class BooleanValue extends BufferedValue
    {
        private final boolean _value;
        
        public BooleanValue(boolean v) { _value = v; }

        public void write(CsvWriter w) throws IOException {
            w.appendValue(_value);
        }
    }
}
