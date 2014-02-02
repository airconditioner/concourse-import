/*
 * t * The MIT License (MIT)
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

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * An {@link Importer} that can handle files with lines that are delimited by a
 * comma.
 * 
 * @author jnelson
 */
public class CsvImporter extends LineImporter {

    /**
     * Return a {@link CsvImporter} that is connected to the server at
     * {@code host} listening on {@code port} and authenticated using
     * {@code username} and {@code password}.
     * 
     * @param host
     * @param port
     * @param username
     * @param password
     * @return the CsvImporter
     */
    public static CsvImporter withConnectionInfo(String host, int port,
            String username, String password) {
        return new CsvImporter(host, port, username, password);
    }

    /**
     * The delimiter that is used to separate fields in the CSV file.
     */
    private static final String DELIMITER = ",";

    /**
     * Construct a new instance.
     * 
     * @param host
     * @param port
     * @param username
     * @param password
     */
    protected CsvImporter(String host, int port, String username,
            String password) {
        super(host, port, username, password);
    }

    @Override
    public String[] parseHeader(String line) {
        return prepareLine(line).split(delimiter());
    }

    @Override
    public final Multimap<String, String> parseLine(String line,
            String... headers) {
        Multimap<String, String> data = LinkedHashMultimap.create();
        String[] toks = prepareLine(line).split(delimiter());
        for (int i = 0; i < toks.length; i++) {
            data.put(headers[i], transformValue(headers[i], toks[i]));
        }
        return data;
    }

    /**
     * This method is provided so the subclass can specify the csv delimiter. By
     * default, {@link #DELIMITER} is used.
     * 
     * @return the delimiter used by this importer
     */
    protected String delimiter() {
        return DELIMITER;
    }

    /**
     * This method is provided so the subclass can transform a {@code value}
     * under {@code header} into something else. This is allows cases where it
     * is necessary to normalize data or convert it to a more compact
     * representation (i.e. string description of enums to ints, etc)
     * 
     * @param header
     * @param line
     * @return the transformed value
     */
    protected String transformValue(String header, String value) {
        return value;
    }

    /**
     * Preprocess {@code line} before parsing it.
     * 
     * @param line
     * @return the processed line.
     */
    private String prepareLine(String line) {
        return line.replaceAll(DELIMITER + " ", DELIMITER);
    }

}
