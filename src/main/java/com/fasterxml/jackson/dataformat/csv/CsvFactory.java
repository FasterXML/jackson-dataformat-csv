package com.fasterxml.jackson.dataformat.csv;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;

import org.codehaus.jackson.*;
import org.codehaus.jackson.format.InputAccessor;
import org.codehaus.jackson.format.MatchStrength;
import org.codehaus.jackson.io.IOContext;

import com.fasterxml.jackson.dataformat.csv.impl.CsvParserBootstrapper;
import com.fasterxml.jackson.dataformat.csv.impl.UTF8Writer;

public class CsvFactory extends JsonFactory
{
    /**
     * Name used to identify CSV format.
     * (and returned by {@link #getFormatName()}
     */
    public final static String FORMAT_NAME_CSV = "CSV";
    
    /**
     * Bitfield (set of flags) of all parser features that are enabled
     * by default.
     */
    final static int DEFAULT_CSV_PARSER_FEATURE_FLAGS = CsvParser.Feature.collectDefaults();

    /**
     * Bitfield (set of flags) of all generator features that are enabled
     * by default.
     */
    final static int DEFAULT_CSV_GENERATOR_FEATURE_FLAGS = CsvGenerator.Feature.collectDefaults();

    // could make it use Platform default too but...
    protected final static char[] DEFAULT_LF = { '\n' };
    
    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */

    protected int _csvParserFeatures = DEFAULT_CSV_PARSER_FEATURE_FLAGS;

    protected int _csvGeneratorFeatures = DEFAULT_CSV_GENERATOR_FEATURE_FLAGS;

    protected char _cfgColumnSeparator = ',';

    protected char _cfgQuoteCharacter = '"';
    
    protected char[] _cfgLineSeparator = DEFAULT_LF;
    
    /*
    /**********************************************************************
    /* Factory construction, configuration
    /**********************************************************************
     */

    /**
     * Default constructor used to create factory instances.
     * Creation of a factory instance is a light-weight operation,
     * but it is still a good idea to reuse limited number of
     * factory instances (and quite often just a single instance):
     * factories are used as context for storing some reused
     * processing objects (such as symbol tables parsers use)
     * and this reuse only works within context of a single
     * factory instance.
     */
    public CsvFactory() { this(null); }

    public CsvFactory(ObjectCodec oc) { super(oc); }
    
    /*
    /**********************************************************
    /* Format detection functionality (since 1.8)
    /**********************************************************
     */
    
    @Override
    public String getFormatName()
    {
        return FORMAT_NAME_CSV;
    }
    
    /**
     * Sub-classes need to override this method (as of 1.8)
     */
    @Override
    public MatchStrength hasFormat(InputAccessor acc) throws IOException
    {
        // !!! TBI -- but how?
        return MatchStrength.INCONCLUSIVE;
    }
    
    /*
    /**********************************************************
    /* Configuration, parser settings
    /**********************************************************
     */

    /**
     * Method for enabling or disabling specified parser feature
     * (check {@link CsvParser.Feature} for list of features)
     */
    public final CsvFactory configure(CsvParser.Feature f, boolean state)
    {
        if (state) {
            enable(f);
        } else {
            disable(f);
        }
        return this;
    }

    /**
     * Method for enabling specified parser feature
     * (check {@link CsvParser.Feature} for list of features)
     */
    public CsvFactory enable(CsvParser.Feature f) {
        _csvParserFeatures |= f.getMask();
        return this;
    }

    /**
     * Method for disabling specified parser features
     * (check {@link CsvParser.Feature} for list of features)
     */
    public CsvFactory disable(CsvParser.Feature f) {
        _csvParserFeatures &= ~f.getMask();
        return this;
    }

    /**
     * Checked whether specified parser feature is enabled.
     */
    public final boolean isEnabled(CsvParser.Feature f) {
        return (_csvParserFeatures & f.getMask()) != 0;
    }

    /*
    /**********************************************************
    /* Configuration, generator settings
    /**********************************************************
     */

    /**
     * Method for enabling or disabling specified generator feature
     * (check {@link org.codehaus.jackson.JsonGenerator.Feature} for list of features)
     *
     * @since 1.2
     */
    public final CsvFactory configure(CsvGenerator.Feature f, boolean state) {
        if (state) {
            enable(f);
        } else {
            disable(f);
        }
        return this;
    }


    /**
     * Method for enabling specified generator features
     * (check {@link org.codehaus.jackson.JsonGenerator.Feature} for list of features)
     */
    public CsvFactory enable(CsvGenerator.Feature f) {
        _csvGeneratorFeatures |= f.getMask();
        return this;
    }

    /**
     * Method for disabling specified generator feature
     * (check {@link org.codehaus.jackson.JsonGenerator.Feature} for list of features)
     */
    public CsvFactory disable(CsvGenerator.Feature f) {
        _csvGeneratorFeatures &= ~f.getMask();
        return this;
    }

    /**
     * Check whether specified generator feature is enabled.
     */
    public final boolean isEnabled(CsvGenerator.Feature f) {
        return (_csvGeneratorFeatures & f.getMask()) != 0;
    }
    
    /*
    /**********************************************************
    /* Overridden parser factory methods
    /**********************************************************
     */

    @Override
    public CsvParser createJsonParser(File f)
        throws IOException, JsonParseException
    {
        return _createJsonParser(new FileInputStream(f), _createContext(f, true));
    }

    @Override
    public CsvParser createJsonParser(URL url)
        throws IOException, JsonParseException
    {
        return _createJsonParser(_optimizedStreamFromURL(url), _createContext(url, true));
    }

    @Override
    public CsvParser createJsonParser(InputStream in)
        throws IOException, JsonParseException
    {
        return _createJsonParser(in, _createContext(in, false));
    }

    //public JsonParser createJsonParser(Reader r)
    
    @Override
    public CsvParser createJsonParser(byte[] data)
        throws IOException, JsonParseException
    {
        return _createJsonParser(data, 0, data.length, _createContext(data, true));
    }
    
    @Override
    public CsvParser createJsonParser(byte[] data, int offset, int len)
        throws IOException, JsonParseException
    {
        return _createJsonParser(data, offset, len, _createContext(data, true));
    }

    /*
    /**********************************************************
    /* Overridden generator factory methods
    /**********************************************************
     */
    
    /**
     *<p>
     * note: co-variant return type
     */
    @Override
    public CsvGenerator createJsonGenerator(OutputStream out, JsonEncoding enc)
        throws IOException
    {
        return createJsonGenerator(out);
    }

    /**
     * Since Csv format always uses UTF-8 internally, no encoding need
     * to be passed to this method.
     */
    @Override
    public CsvGenerator createJsonGenerator(OutputStream out) throws IOException
    {
        // false -> we won't manage the stream unless explicitly directed to
        IOContext ctxt = _createContext(out, false);
        return _createJsonGenerator(ctxt, _createWriter(out, JsonEncoding.UTF8, ctxt));
    }
    
    /*
    /******************************************************
    /* Overridden internal factory methods
    /******************************************************
     */

    //protected IOContext _createContext(Object srcRef, boolean resourceManaged)

    /**
     * Overridable factory method that actually instantiates desired
     * parser.
     */
    @Override
    protected CsvParser _createJsonParser(InputStream in, IOContext ctxt)
        throws IOException, JsonParseException
    {
        /*
        return new CsvParserBootstrapper(ctxt, in).constructParser(_parserFeatures,
                        _csvParserFeatures, _objectCodec, _rootByteSymbols);
                        */
        return null;
    }

    /**
     * Overridable factory method that actually instantiates desired
     * parser.
     */
    @Override
    protected JsonParser _createJsonParser(Reader r, IOContext ctxt)
        throws IOException, JsonParseException
    {
        throw new UnsupportedOperationException("Can not create generator for non-byte-based target");
    }

    /**
     * Overridable factory method that actually instantiates desired
     * parser.
     */
    @Override
    protected CsvParser _createJsonParser(byte[] data, int offset, int len, IOContext ctxt)
        throws IOException, JsonParseException
    {
        /*
        return new CsvParserBootstrapper(ctxt, data, offset, len).constructParser(_parserFeatures,
                        _csvParserFeatures, _objectCodec, _rootByteSymbols);
                        */
        return null;
    }

    /**
     * Overridable factory method that actually instantiates desired
     * generator.
     */
    @Override
    protected JsonGenerator _createJsonGenerator(Writer out, IOContext ctxt)
        throws IOException
    {
        return _createJsonGenerator(ctxt, out);
    }

    @Override
    protected Writer _createWriter(OutputStream out, JsonEncoding enc, IOContext ctxt) throws IOException
    {
        if (enc == JsonEncoding.UTF8) {
            return new UTF8Writer(ctxt, out);
        }
        return new OutputStreamWriter(out, enc.getJavaName());
    }
    
    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */
    
    protected CsvGenerator _createJsonGenerator(IOContext ctxt, Writer out)
        throws IOException
    {
        int feats = _csvGeneratorFeatures;
        CsvGenerator gen = new CsvGenerator(ctxt, _generatorFeatures, feats,
                _objectCodec, out,
                _cfgColumnSeparator, _cfgQuoteCharacter, _cfgLineSeparator);
        // any other initializations? No?
        return gen;
    }
}
