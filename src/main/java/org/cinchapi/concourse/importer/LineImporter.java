/*
 * The MIT License (MIT)
 * 
 * Copyright (c) 2014 Jeff Nelson, Cinchapi Software Collective
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.cinchapi.concourse.importer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;


import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

/**
 * A {@link LineImporter} is one that handles files where each line represents a
 * single collection of data that should be imported into one or more (possibly
 * existing) records.
 * 
 * @author jnelson
 */
public abstract class LineImporter extends AbstractImporter {

    /**
     * Construct a new instance.
     * 
     * @param host
     * @param port
     * @param username
     * @param password
     */
    protected LineImporter(String host, int port, String username,
            String password) {
        super(host, port, username, password);
    }

    @Override
    public final Collection<ImportResult> importFile(String file) {
        return importFile(file, null); // for the default cause,
                                       // assume that we want to
                                       // import each line into a
                                       // new record
    }

    /**
     * Import the data in {@code file}.
     * <p>
     * Each individual line of the file will be possibly split by some
     * delimiter, processed as a collection and added to one or more records in
     * {@link Concourse}.
     * </p>
     * <p>
     * <strong>Note</strong> that if {@code resolveKey} is specified, an attempt
     * will be made to add the data in each line into the existing records that
     * are found using {@code resolveKey} and its corresponding value in the
     * line.
     * </p>
     * 
     * @param file
     * @param resolveKey
     * @return a collection of {@link ImportResult} objects that describes the
     *         records created/affected from the import and whether any errors
     *         occurred.
     */
    public Collection<ImportResult> importFile(String file,
            @Nullable String resolveKey) {
        List<ImportResult> results = Lists.newArrayList();
        String[] keys = header();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                if(keys == null) {
                    keys = parseHeader(line);
                    log.info("Processed header: " + line);
                }
                else {
                    ImportResult result = doImport(parseLine(line, keys),
                            resolveKey);
                    results.add(result);
                    log.info(MessageFormat
                            .format("Imported {0} into record(s) {1} with {2} error(s)",
                                    line, result.getRecords(),
                                    result.getErrorCount()));
                }
            }
            reader.close();
            return results;
        }
        catch (IOException e) {
            throw Throwables.propagate(e);
        }

    }

    /**
     * This method is provided so the subclass can provide an ordered array of
     * headers if they are not provided in the file as the first line.
     * 
     * @return the header information
     */
    protected String[] header() {
        return null;
    }

    /**
     * Parse the header from {@code line} into a set of ordered keys. The
     * subclass can ignore this method and return an empty array or {@code null}
     * if it has overridden {@link #header()}.
     * 
     * @param line
     * @return the keys in the header
     */
    // subclass should define its own delimiter
    protected abstract String[] parseHeader(String line);

    /**
     * Parse the data from {@code line} into a multimap from key (header) to
     * value.
     * 
     * @param line
     * @param headers
     * @return the line data
     */
    // subclass should define its own delimiter
    protected abstract Multimap<String, String> parseLine(String line,
            String... headers);

    /**
     * This method allows the subclass to define dynamic intermediary
     * transformations to data to better prepare it for import. This method is
     * called before the raw string data is converted to a Java object. There
     * are several instances for which the subclass should use this method:
     * <p>
     * <h2>Specifying Link Resolution</h2>
     * The importer will convert raw data of the form
     * <code>@&lt;key&gt;@value@&lt;key&gt;@</code> into a Link to all the
     * records where key equals value in Concourse. For this purpose, the
     * subclass can convert the raw value to this form using the
     * {@link #transformValueToResolvableLink(String, String)} method.
     * </p>
     * <p>
     * <h2>Normalizing Data</h2>
     * It may be desirable to normalize the raw data before input. For example,
     * the subclass may wish to convert all strings to a specific case, or
     * sanitize inputs, etc.
     * </p>
     * <p>
     * <h2>Compacting Representation</h2>
     * If a column in a file contains a enumerated set of string values, it may
     * be desirable to transform the values to a string representation of a
     * number so that, when converted, the data is more compact and takes up
     * less space.
     * </p>
     * 
     * @param header
     * @param value
     * @return the transformed value
     */
    protected String transformValue(String header, String value) {
        return value;
    }
}
