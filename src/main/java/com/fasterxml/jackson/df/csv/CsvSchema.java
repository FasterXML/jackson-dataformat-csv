package com.fasterxml.jackson.df.csv;

import org.codehaus.jackson.FormatSchema;

    /**
     * Simple {@link FormatSchema} sub-type which supports simple
     * (non-nested) tabular data format, where data consists of rows
     * of homogenous data, where typing is positional.
     * This schema can be used as basetype for many simple tabular
     * data format; for example CSV schema can be based on this.
     * Names for columns are optional; and while types are not optional
     * it is possible to use basic {@link java.lang.Object} as placeholder
     * if more specific typ is not available.
     *
     * @since 1.9
     */
public class CsvSchema 
    implements FormatSchema
{

        public String getSchemaType() {
            return "CSV";
        }

}
