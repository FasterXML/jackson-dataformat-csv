package com.fasterxml.jackson.dataformat.csv;

import com.fasterxml.jackson.core.JsonEncoding;

/**
 * Enumeration that defines commond encodings that can be used
 * for CSV content; similar to how
 * {@link com.fasterxml.jackson.core.JsonEncoding} defines encodings
 * for JSON.
 * Note, however, that there is no specification on encodings that
 * CSV files can take; and most commonly only UTF-8 or Latin-1
 * is used.
 *<p>
 * NOTE: as of Jackson 2.2, this enumeration is <b>not used</b>
 * for anything -- it is more of a placeholder for possible future
 * functionality.
 *<p>
 * Note: if application want to explicitly disregard Encoding
 * limitations (to read in content encoded using an encoding not
 * listed as allowed), they can use {@link java.io.Reader} 
 * instances as input
 */
public enum CsvEncoding {
   UTF8("UTF-8", false), // N/A for big-endian, really
       UTF16_BE("UTF-16BE", true),
       UTF16_LE("UTF-16LE", false),
       UTF32_BE("UTF-32BE", true),
       UTF32_LE("UTF-32LE", false),
       LATIN1("ISO-8859-1", false) // single-byte, no endianness
       ;
   
   protected final String _javaName;

   protected final boolean _bigEndian;
   
   CsvEncoding(String javaName, boolean bigEndian)
   {
       _javaName = javaName;
       _bigEndian = bigEndian;
   }

   public static CsvEncoding from(JsonEncoding enc)
   {
       switch (enc) {
       case UTF8:
           return UTF8;
       case UTF16_BE:
           return UTF16_BE;
       case UTF16_LE:
           return UTF16_LE;
       case UTF32_BE:
           return UTF32_BE;
       case UTF32_LE:
           return UTF32_LE;
       }
       throw new IllegalStateException();
   }
   
   /**
    * Method for accessing encoding name that JDK will support.
    *
    * @return Matching encoding name that JDK will support.
    */
   public String getJavaName() { return _javaName; }

   /**
    * Whether encoding is big-endian (if encoding supports such
    * notion). If no such distinction is made (as is the case for
    * {@link #UTF8}), return value is undefined.
    *
    * @return True for big-endian encodings; false for little-endian
    *   (or if not applicable)
    */
   public boolean isBigEndian() { return _bigEndian; }
}
