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

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * <p>
 * An {@link Importer} that can handle generic CSV files that have header
 * information in the first line. It is advisable to extend this class for CSV
 * data that has special requirements. This class makes some general assumptions
 * that can be configured in a subclass:
 * </p>
 * <h2>Header</h2>
 * <p>
 * It is assumed that the first line of a CSV file contains the header. If that
 * is not the case, the subclass can return an ordered array of header keys from
 * the {@link #header()} method.
 * </p>
 * <h2>Delimiter</h2>
 * <p>
 * It is assumed that values in a line are comma separated. If that is not the
 * case, the subclass can specify a different rule in the {@link #delimiter()}
 * method.
 * </p>
 * <h2>Transforming Values</h2>
 * <p>
 * It is assumed that the original file data is correct. If that is not the
 * case, the subclass can selectively transform some values in the
 * {@link #transformValue(String, String)} method. For example, it might be
 * desirable specify link resolution, compact data or normalize data.
 * </p>
 * 
 * @author jnelson
 */
public class GeneralCsvImporter extends FileLineImporter {

    /**
     * Return a {@link GeneralCsvImporter} that is connected to the server at
     * {@code host} listening on {@code port} and authenticated using
     * {@code username} and {@code password}.
     * 
     * @param host
     * @param port
     * @param username
     * @param password
     * @return the CsvImporter
     */
    public static GeneralCsvImporter withConnectionInfo(String host, int port,
            String username, String password) {
        return new GeneralCsvImporter(host, port, username, password);
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
    protected GeneralCsvImporter(String host, int port, String username,
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
     * @param key
     * @param value
     * @return the transformed value
     */
    protected String transformValue(String key, String value) {
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
