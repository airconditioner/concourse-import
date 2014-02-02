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
import javax.annotation.concurrent.Immutable;

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
public abstract class LineImporter extends AbstractImporter {

    /**
     * The component of a resolvable link symbol that comes before the
     * resolvable key specification in the raw data.
     */
    protected static final String RAW_RESOLVABLE_LINK_SYMBOL_PREPEND = "@<"; // visible
                                                                             // for
                                                                             // testing

    /**
     * The component of a resolvable link symbol that comes after the
     * resolvable key specification in the raw data.
     */
    protected static final String RAW_RESOLVABLE_LINK_SYMBOL_APPEND = ">@"; // visible
                                                                            // for
                                                                            // testing

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
     * Analyze {@code value} and convert it to the appropriate Java primitive or
     * Object
     * 
     * @param value
     * @return the converted value
     */
    protected final Object convert(String value) { // visible for testing
        if(value.matches("\"([^\"]+)\"|'([^']+)'")) { // keep value as
                                                      // string since its
                                                      // between single or
                                                      // double quotes
            return value.substring(1, value.length() - 1);
        }
        else if(value.matches(MessageFormat.format(
                "{0}[A-Za-z0-9]{1}[A-Za-z0-9]+",
                RAW_RESOLVABLE_LINK_SYMBOL_PREPEND,
                RAW_RESOLVABLE_LINK_SYMBOL_APPEND))) {
            /*
             * I want to specify a link in the raw data in terms of a
             * value in the raw data instead of the Concourse primary key
             * 
             * Example
             * account_number | customer | account_type
             * 12345 | 678 | SAVINGS
             * 
             * Lets assume in this scenario, I've imported customer data. Now, I
             * want to link the "customer" key in each account record that is
             * created to the previously created customer records. By specifying
             * the raw customer value as @<customer_id>@678@<customer_id>@ I am
             * saying that the system should find all the records where
             * customer_id is equal to 678 and link the "customer" key in the
             * account record that is being created to all those records.
             * 
             * FURTHERMORE, the transformation of 678 to
             * <code> @<customer_id>@678@<customer_id>@</code> can be done in
             * the transformValue() method
             */
            String[] parts = value.split(RAW_RESOLVABLE_LINK_SYMBOL_PREPEND, 2)[0]
                    .split(RAW_RESOLVABLE_LINK_SYMBOL_APPEND, 2);
            String key = parts[0];
            Object theValue = convert(parts[1]);
            return new ResolvableLink(key, theValue);
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

    /**
     * Transform the {@code rawValue} into a resolvable link specification using
     * the {@code resolvableKey}.
     * 
     * @param resolvableKey
     * @param rawValue
     * @return the transformed value.
     */
    protected final String transformValueToResolvableLink(String resolvableKey,
            String rawValue) {
        return MessageFormat.format("{0}{1}{0}", MessageFormat.format(
                "{0}{1}{2}", RAW_RESOLVABLE_LINK_SYMBOL_PREPEND, resolvableKey,
                RAW_RESOLVABLE_LINK_SYMBOL_APPEND), rawValue);
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
    // This method must be synchronized so that multiple import threads using
    // the same client do not try to start competing transactions
    private synchronized ImportResult importLineData(
            Multimap<String, String> data, @Nullable String resolveKey) {
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
        concourse.stage();
        for (String key : data.keySet()) {
            for (String rawValue : data.get(key)) {
                if(!Strings.isNullOrEmpty(rawValue)) { // do not waste time
                                                       // sending empty values
                                                       // over the wire
                    Object convertedValue = convert(rawValue);
                    List<Object> values = Lists.newArrayList();
                    if(convertedValue instanceof ResolvableLink) {
                        // Find all the records that resolve and create a Link
                        // to those records.
                        for (long record : concourse.find(
                                ((ResolvableLink) convertedValue).key,
                                Operator.EQUALS,
                                ((ResolvableLink) convertedValue).value)) {
                            values.add(Link.to(record));
                        }
                    }
                    else {
                        values.add(convertedValue);
                    }
                    for (long record : records) {
                        for (Object value : values) {
                            if(!concourse.add(key, value, record)) {
                                result.addError(MessageFormat.format(
                                        "Could not import {0} AS {1} IN {2}",
                                        key, value, record));
                            }
                        }

                    }
                }
            }
        }
        if(concourse.commit()) {
            return result;
        }
        else {
            // TODO add some max number of retries before giving up?
            log.warn(
                    "Error trying to commit import of {0}. Attempting to retry the import...",
                    data);
            return importLineData(data, resolveKey);
        }
    }

    /**
     * A special class that is used to indicate that the record to which a Link
     * should point must be resolved by finding all records that have a
     * specified key equal to a specified value.
     * 
     * @author jnelson
     */
    @Immutable
    private final class ResolvableLink {

        private final String key;
        private final Object value;

        /**
         * Construct a new instance.
         * 
         * @param key
         * @param value
         */
        private ResolvableLink(String key, Object value) {
            this.key = key;
            this.value = value;
        }

    }
}
