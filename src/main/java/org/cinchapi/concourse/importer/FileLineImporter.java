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
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;

import org.cinchapi.concourse.Concourse;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

/**
 * A {@link FileLineImporter} is one that handles data from a file that can be
 * delimited into one or more lines. Each line is a single
 * group of data that can be converted to a multimap and imported into one or
 * more records in Concourse.
 * <p>
 * This abstract class handles the logic of splitting the file into a collection
 * of lines and asynchronously importing those lines. The subclass must delimit
 * each of those line and parse the appropriate headers and groups using the
 * {@link #parseHeader(String)} and {@link #parseLine(String, String...)}
 * </p>
 * 
 * @author jnelson
 */
public abstract class FileLineImporter extends AbstractImporter {

    /**
     * Construct a new instance.
     * 
     * @param host
     * @param port
     * @param username
     * @param password
     */
    protected FileLineImporter(String host, int port, String username,
            String password) {
        super(Concourse.connect(host, port, username, password));
    }

    /**
     * Construct a new instance.
     * 
     * @param host
     * @param port
     * @param username
     * @param password
     * @param environment
     */
    protected FileLineImporter(String host, int port, String username,
            String password, String environment) {
        super(Concourse.connect(host, port, username, password, environment));
    }

    /**
     * Import the lines in {@code file}.
     * <p>
     * Each individual line of the file will be possibly split by some
     * delimiter, processed as a group and added to one or more records in
     * Concourse.
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
    @Override
    public Collection<ImportResult> handleFileImport(String file,
            @Nullable final String resolveKey) {
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
                    Multimap<String, String> data = parseLine(line, keys);
                    ImportResult result = importGroup(data, resolveKey);
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
        catch (Exception e) {
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
     * A {@link Runnable} that is launched from the {@link #importFile} and
     * passed the line to import and the order array of keys.
     * 
     * @author jnelson
     */
    protected abstract class ImportRunnable implements Runnable {
        protected final String line;
        protected final String[] keys;

        /**
         * Construct a new instance.
         * 
         * @param line
         * @param keys
         */
        public ImportRunnable(String line, String[] keys) {
            this.line = line;
            this.keys = keys;
        }

    }
}
