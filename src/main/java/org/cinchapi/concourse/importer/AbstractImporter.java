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
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import org.cinchapi.concourse.Concourse;
import org.cinchapi.concourse.Link;
import org.cinchapi.concourse.thrift.Operator;
import org.cinchapi.concourse.util.Convert;
import org.cinchapi.concourse.util.Convert.ResolvableLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

/**
 * Base implementation of the {@link Importer} interface that handles the work
 * of importing data into Concourse. The subclass that extends this
 * implementation only needs to worry about implementing the
 * {@link #handleFileImport(String)} method and converting each group of raw
 * data (i.e. a line in csv file) into a {@link Multimap} that is passed to the
 * {@link #importGroup(Multimap, String)} method.
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
 * using the {@link #importGroup(Concourse, Multimap)} method.
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
 * {@link Convert#stringToResolvableLinkSpecification(String, String)} on the
 * raw data before converting it to a multimap.
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
     * A Logger that is available for the subclass to log helpful messages.
     */
    protected Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The connection to Concourse.
     */
    private final Concourse concourse;

    /**
     * Construct a new instance.
     * 
     * @param concourse
     */
    protected AbstractImporter(Concourse concourse) {
        this.concourse = concourse;
    }

    @Override
    public Collection<ImportResult> importFile(String file) {
        return importFile(file, null);
    }

    @Override
    public final Collection<ImportResult> importFile(String file,
            @Nullable String resolveKey) {
        concourse.stage();
        Collection<ImportResult> results = handleFileImport(file, resolveKey);
        if(concourse.commit()) {
            return results;
        }
        else {
            throw new RuntimeException("Could not import " + file);
        }
    }

    /**
     * Split {@code file} into the appropriate groups, and call
     * {@link #importGroup(Multimap)} to get the data into Concourse.
     * 
     * @param file
     * @param resolveKey
     * @return the results of the import
     */
    protected abstract Collection<ImportResult> handleFileImport(String file,
            @Nullable String resolveKey);

    /**
     * Import a single group of {@code data} into a new record in
     * {@code concourse}.
     * 
     * @param data
     * @return an {@link ImportResult} object that describes the records
     *         created/affected from the import and whether any errors occurred.
     */
    protected final ImportResult importGroup(Multimap<String, String> data) {
        return importGroup(data, null);
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
     * @param data
     * @param resolveKey
     * @return an {@link ImportResult} object that describes the records
     *         created/affected from the import and whether any errors occurred.
     */
    protected final ImportResult importGroup(Multimap<String, String> data,
            @Nullable String resolveKey) {
        // Determine import record(s)
        Set<Long> records = Sets.newHashSet();
        for (String resolveValue : data.get(resolveKey)) {
            records = Sets.union(
                    records,
                    concourse.find(resolveKey, Operator.EQUALS,
                            Convert.stringToJava(resolveValue)));
            records = Sets.newHashSet(records); // must make copy because
                                                // previous method returns
                                                // immutable view
        }
        if(records.isEmpty()) {
            records.add(concourse.create());
        }
        // Iterate through the data and add it to Concourse
        ImportResult result = ImportResult.newImportResult(data, records);
        for (String key : data.keySet()) {
            for (String rawValue : data.get(key)) {
                if(!Strings.isNullOrEmpty(rawValue)) { // do not waste time
                                                       // sending empty
                                                       // values
                                                       // over the wire
                    Object convertedValue = Convert.stringToJava(rawValue);
                    List<Object> values = Lists.newArrayList();
                    if(convertedValue instanceof ResolvableLink) {
                        // Find all the records that resolve and create a
                        // Link to those records.
                        for (long record : concourse.find(
                                ((ResolvableLink) convertedValue).getKey(),
                                Operator.EQUALS,
                                ((ResolvableLink) convertedValue).getValue())) {
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
        return result;
    }

}
