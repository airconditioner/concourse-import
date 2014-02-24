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

import java.text.MessageFormat;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.cinchapi.concourse.Concourse;
import org.cinchapi.concourse.Link;
import org.cinchapi.concourse.annotate.PackagePrivate;
import org.cinchapi.concourse.thrift.Operator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

/**
 * Base implementation of the {@link Importer} interface that handles the work
 * of importing data into Concourse. The subclass that extends this
 * implementation only needs to worry about providing connection(s) to Concourse
 * and converting each group of raw data (i.e. a line in csv file) into a
 * {@link Multimap} that is passed to the
 * {@link #doImport(Concourse, Multimap, String)} method.
 * <p>
 * This implementation does not mandate anything about the structure of the raw
 * data other than the assumption that it can be transformed into one or more
 * multi-mappings of keys to values (called a <strong>group</strong>). All data
 * in a group is imported into the appropriate record(s) together. Since this
 * requirement is very lightweight, it is possible to extend this implementation
 * to handle things like CSV files, XML files, SQL dumps, email documents, etc.
 * </p>
 * <h2>Import into new record</h2>
 * <p>
 * By default, all the data in a group is converted into one new record when
 * using the {@link #doImport(Concourse, Multimap)} method.
 * </p>
 * <h2>Import into existing record(s)</h2>
 * <p>
 * It is possible to import all the data in a group into one or more existing
 * records by specifying a <strong>resolveKey</strong> in the
 * {@link #doImport(Concourse, Multimap, String)} method.
 * </p>
 * <p>
 * For each group, the importer will find all the records that have at least one
 * of the same values mapped from {@code resolveKey} as defined in the group
 * data. The importer will then import all the group data into all of those
 * record.
 * </p>
 * <p>
 * <strong>NOTE:</strong> It is not possible to specify the Primary Key of a
 * record as the resolveKey. The Primary Key is metadata which isn't necessarily
 * known to the raw data. Therefore, we prefer that record resolution happens in
 * terms that are known by the raw data.
 * </p>
 * <h2>Specifying links in raw data</h2>
 * <p>
 * It is possible to link the record into which group data is imported to
 * another existing record. There is a special format for specifying
 * <strong>resolvable links</strong> in raw data. The subclass can convert the
 * raw data to that format by calling the
 * {@link #transformValueToResolvableLink(String, String)} on the raw data
 * before converting it to a multimap.
 * </p>
 * <p>
 * When the importer encounters a {@link ResolvableLink}, similar to a
 * resolveKey, it finds all the records who have the value for the specified key
 * and links the record into which the group data is imported to all those
 * records.
 * </p>
 * <h4>Example</h4>
 * <table>
 * <tr>
 * <th>account_number</th>
 * <th>customer</th>
 * <th>account_type</th>
 * </tr>
 * <tr>
 * <td>12345</td>
 * <td>@&lt;customer_id&gt;@678@&lt;customer_id&gt;@</td>
 * <td>SAVINGS</td>
 * </tr>
 * </table>
 * <p>
 * Lets assume in this scenario, I've imported customer data. Now, I want to
 * link the "customer" key in each account record that is created to the
 * previously created customer records. By specifying the raw customer value as
 * 
 * {@code @<customer_id>@678@<customer_id>@} I am saying that the system should
 * find all the records where customer_id is equal to 678 and link the
 * "customer" key in the account record that is being created to all those
 * records.
 * <p>
 * 
 * @author jnelson
 */
public abstract class AbstractImporter implements Importer {

    /**
     * Transform the {@code rawValue} into a resolvable link specification using
     * the {@code resolvableKey}. This method should be called by the subclass
     * on raw data <strong>before</strong> converting it to a multimap. For a
     * resolvable link, it is desirable to have the multimap given to the
     * {@link #doImport(Multimap)} methods map the linkable key to a string that
     * describes the resolvable record. The format of that string is returned
     * from this method.
     * 
     * @param resolvableKey
     * @param rawValue
     * @return the transformed value.
     */
    protected static String transformValueToResolvableLink(
            String resolvableKey, String rawValue) {
        return MessageFormat.format("{0}{1}{0}", MessageFormat.format(
                "{0}{1}{2}", RAW_RESOLVABLE_LINK_SYMBOL_PREPEND, resolvableKey,
                RAW_RESOLVABLE_LINK_SYMBOL_APPEND), rawValue);
    }

    /**
     * Analyze {@code value} and convert it to the appropriate Java primitive or
     * Object. This method is called internally from the
     * {@link #doImport(Multimap)} methods to convert the raw string values in
     * the multimap to the actual data that is imported into Concourse.
     * 
     * @param value
     * @return the converted value
     */
    @PackagePrivate
    static Object convert(String value) { // visible for testing, should only be
                                          // called internally
        if(value.matches("\"([^\"]+)\"|'([^']+)'")) { // keep value as
                                                      // string since its
                                                      // between single or
                                                      // double quotes
            return value.substring(1, value.length() - 1);
        }
        else if(value.matches(MessageFormat.format("{0}{1}{0}", MessageFormat
                .format("{0}{1}{2}", RAW_RESOLVABLE_LINK_SYMBOL_PREPEND, ".+",
                        RAW_RESOLVABLE_LINK_SYMBOL_APPEND), ".+"))) {
            String[] parts = value.split(RAW_RESOLVABLE_LINK_SYMBOL_PREPEND, 3)[1]
                    .split(RAW_RESOLVABLE_LINK_SYMBOL_APPEND, 2);
            String key = parts[0];
            Object theValue = convert(parts[1]);
            return new ResolvableLink(key, theValue);
        }
        else if(value.matches("@-?[0-9]+@")) {
            return Link.to(Long.parseLong(value.replace("@", "")));
        }
        else if(value.equalsIgnoreCase("true")) {
            return true;
        }
        else if(value.equalsIgnoreCase("false")) {
            return false;
        }
        else if(value.matches("-?[0-9]+\\.[0-9]+D")) { // Must append "D" to end
                                                       // of string in order to
                                                       // force a double
            return Double.valueOf(value.substring(0, value.length() - 1));
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
     * The component of a resolvable link symbol that comes before the
     * resolvable key specification in the raw data.
     */
    @PackagePrivate
    static final String RAW_RESOLVABLE_LINK_SYMBOL_PREPEND = "@<"; // visible
                                                                   // for
                                                                   // testing

    /**
     * The component of a resolvable link symbol that comes after the
     * resolvable key specification in the raw data.
     */
    @PackagePrivate
    static final String RAW_RESOLVABLE_LINK_SYMBOL_APPEND = ">@"; // visible
                                                                  // for
                                                                  // testing

    /**
     * A Logger that is available for the subclass to log helpful messages.
     */
    protected Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Import a single group of {@code data} into a new record in
     * {@code concourse}.
     * 
     * @param concourse
     * @param data
     * @return an {@link ImportResult} object that describes the records
     *         created/affected from the import and whether any errors occurred.
     */
    protected ImportResult doImport(Concourse concourse,
            Multimap<String, String> data) {
        return doImport(concourse, data, null);
    }

    /**
     * Import a single group of {@code data} (i.e. a line in a csv file) into
     * {@code concourse}.
     * <p>
     * If {@code resolveKey} is specified, it is possible that the {@code data}
     * will be added to more than one existing record. It is guaranteed that an
     * attempt will be made to add the data to at least one (possibly) new
     * record.
     * </p>
     * 
     * @param concourse
     * @param data
     * @param resolveKey
     * @return an {@link ImportResult} object that describes the records
     *         created/affected from the import and whether any errors occurred.
     */
    protected ImportResult doImport(Concourse concourse,
            Multimap<String, String> data, @Nullable String resolveKey) {
        // Determine import record(s)
        Set<Long> records = Sets.newHashSet();
        for (String resolveValue : data.get(resolveKey)) {
            records = Sets.union(records, concourse.find(resolveKey,
                    Operator.EQUALS, convert(resolveValue)));
        }
        if(records.isEmpty()) {
            records.add(concourse.create());
        }
        // Iterate through the data and add it to Concourse
        ImportResult result = ImportResult.newImportResult(data, records);
        concourse.stage();
        for (String key : data.keySet()) {
            for (String rawValue : data.get(key)) {
                if(!Strings.isNullOrEmpty(rawValue)) { // do not waste time
                                                       // sending empty
                                                       // values
                                                       // over the wire
                    Object convertedValue = convert(rawValue);
                    List<Object> values = Lists.newArrayList();
                    if(convertedValue instanceof ResolvableLink) {
                        // Find all the records that resolve and create a
                        // Link
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
            return doImport(concourse, data, resolveKey);
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
