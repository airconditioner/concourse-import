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

import static org.cinchapi.concourse.importer.AbstractImporter.RAW_RESOLVABLE_LINK_SYMBOL_APPEND;
import static org.cinchapi.concourse.importer.AbstractImporter.RAW_RESOLVABLE_LINK_SYMBOL_PREPEND;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.cinchapi.concourse.Concourse;
import org.cinchapi.concourse.Link;
import org.cinchapi.concourse.thrift.Operator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

/**
 * Abstract implementation of the {@link Importer} interface. This class
 * provides a handler to {@link Concourse} that is constructed using the
 * credentials in {@code concourse_client.prefs}.
 * 
 * @author jnelson
 */
abstract class AbstractImporter implements Importer {

    /**
     * Analyze {@code value} and convert it to the appropriate Java primitive or
     * Object
     * 
     * @param value
     * @return the converted value
     */
    protected static final Object convert(String value) { // visible for testing
        if(value.matches("\"([^\"]+)\"|'([^']+)'")) { // keep value as
                                                      // string since its
                                                      // between single or
                                                      // double quotes
            return value.substring(1, value.length() - 1);
        }
        else if(value.matches(MessageFormat.format("{0}{1}{0}", MessageFormat
                .format("{0}{1}{2}", RAW_RESOLVABLE_LINK_SYMBOL_PREPEND, ".+",
                        RAW_RESOLVABLE_LINK_SYMBOL_APPEND), ".+"))) {
            // format: @<key>@value@<key>@
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
            String[] parts = value.split(RAW_RESOLVABLE_LINK_SYMBOL_PREPEND, 3)[1]
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
     * Transform the {@code rawValue} into a resolvable link specification using
     * the {@code resolvableKey}.
     * 
     * @param resolvableKey
     * @param rawValue
     * @return the transformed value.
     */
    protected static final String transformValueToResolvableLink(
            String resolvableKey, String rawValue) {
        // format: @<key>@value@<key>@
        return MessageFormat.format("{0}{1}{0}", MessageFormat.format(
                "{0}{1}{2}", RAW_RESOLVABLE_LINK_SYMBOL_PREPEND, resolvableKey,
                RAW_RESOLVABLE_LINK_SYMBOL_APPEND), rawValue);
    }

    /**
     * The component of a resolvable link symbol that comes before the
     * resolvable key specification in the raw data.
     */
    protected static final String RAW_RESOLVABLE_LINK_SYMBOL_PREPEND = "@<";

    /**
     * The component of a resolvable link symbol that comes after the
     * resolvable key specification in the raw data.
     */
    protected static final String RAW_RESOLVABLE_LINK_SYMBOL_APPEND = ">@";

    /**
     * A handler to the {@link Concourse} where the data will be imported.
     */
    protected final Concourse concourse;

    /**
     * A Logger that is available for the subclass to log helpful messages.
     */
    protected Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Construct a new instance.
     * 
     * @param host
     * @param port
     * @param username
     * @param password
     */
    protected AbstractImporter(String host, int port, String username,
            String password) {
        this.concourse = Concourse.connect(host, port, username, password);
    }

    /**
     * Import a single collection of content (i.e. a line in a csv file) that is
     * represented by the {@code data}.
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
    // TODO: should there be a separate client per import attempt?
    protected synchronized ImportResult doImport(Multimap<String, String> data,
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
            log.warn("Error trying to commit import of {0}. "
                    + "Attempting to retry the import...", data);
            return doImport(data, resolveKey);
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
    protected static final class ResolvableLink { // visible for testing

        protected final String key;
        protected final Object value;

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
