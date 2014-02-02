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
import java.util.Set;

import javax.annotation.Nullable;

import org.cinchapi.concourse.Link;
import org.cinchapi.concourse.thrift.Operator;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

/**
 * A {@link LineImporter} is one that handles files where each line represents a
 * single collection of data that should be imported into one or more (possibly
 * existing) records.
 * 
 * @author jnelson
 */
abstract class LineImporter extends AbstractImporter {

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
                    ImportResult result = importLineData(parseLine(line, keys),
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
     * This method is provided so the subclass can provide an ordered array of
     * headers if they are not provided in the file as the first line.
     * 
     * @return the header information
     */
    protected String[] header() {
        return null;
    }

    /**
     * Analyze {@code value} and convert it to the appropriate Java primitive or
     * Object
     * 
     * @param value
     * @return the converted value
     */
    protected final Object convert(String value) { // visible for testing
        if(value.matches("\"([^\"]+)\"|'([^']+)'")) {
            return value.substring(1, value.length() - 1);
        }
        else if(value.matches("@[0-9]+@")) {
            return Link.to(Long.parseLong(value.replace("@", "")));
        }
        else if(value.equalsIgnoreCase("true")) {
            return true;
        }
        else if(value.equalsIgnoreCase("false")) {
            return false;
        }
        else {
            Class<?>[] classes = { Integer.class, Long.class, Float.class,
                    Double.class };
            for (Class<?> clazz : classes) {
                try {
                    return clazz.getMethod("valueOf", String.class).invoke(
                            null, value);
                }
                catch (Exception e) {
                    if(e instanceof NumberFormatException
                            || e.getCause() instanceof NumberFormatException) {
                        continue;
                    }
                }
            }
            return value;
        }
    }

    /**
     * Import a single line that has been transformed into {@code data}.
     * <p>
     * If {@code resolveKey} is specified, it is possible that the {@code data}
     * will be added to more than one existing record. It is guaranteed that an
     * attempt will be made to add the data to at least one (possibly) new
     * record.
     * </p>
     * 
     * @param data
     * @param resolveKey
     * @return an {@link ImportResult} object that describes the records
     *         created/affected from the import and whether any errors occurred.
     */
    private ImportResult importLineData(Multimap<String, String> data,
            @Nullable String resolveKey) {
        // Attempt to resolve the data into one or more existing records,
        // otherwise create a new record
        Collection<String> resolveValues;
        String resolveValue;
        Set<Long> records;
        if(!Strings.isNullOrEmpty(resolveKey)
                && (resolveValues = data.get(resolveKey)).size() == 1
                && Strings.isNullOrEmpty((resolveValue = resolveValues
                        .iterator().next()))) {
            records = concourse.find(resolveKey, Operator.EQUALS, resolveValue);
        }
        else {
            records = Sets.newHashSet(concourse.create());
        }
        // Iterate through the data and add it to Concourse
        ImportResult result = ImportResult.newImportResult(data, records);
        for (String key : data.keySet()) {
            for (String rawValue : data.get(key)) {
                if(!Strings.isNullOrEmpty(rawValue)) { // do not waste time
                                                       // sending empty values
                                                       // over the wire
                    Object value = convert(rawValue);
                    for (long record : records) {
                        if(!concourse.add(key, value, record)) {
                            result.addError(MessageFormat.format(
                                    "Could not import {0} AS {1} IN {2}", key,
                                    value, record));
                        }
                    }
                }
            }
        }
        return result;

    }
}
