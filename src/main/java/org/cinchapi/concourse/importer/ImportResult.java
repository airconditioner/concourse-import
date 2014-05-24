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

import java.util.List;
import java.util.Set;

import javax.annotation.concurrent.Immutable;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

/**
 * An ImportResult contains information about the nature of an attempt to import
 * data into one or more records (i.e. did the import succeed, etc).
 *
 * @author jnelson
 */
@Immutable
public final class ImportResult {

    // NOTE: This class does not define hashCode(), equals() or toString()
    // because the defaults are the desired behaviour

    /**
     * Return an {@link ImportResult} that describes what occurs during an
     * attempt to import {@code importData} into {@code record}.
     *
     * @param importData
     * @param records
     * @return the ImportRecordResult
     */
    public static ImportResult newImportResult(
            Multimap<String, String> importData, Set<Long> records) {
        return new ImportResult(importData, records);
    }

    /**
     * A collection of strings describing errors that occurred during the
     * import.
     */
    private final List<String> errors = Lists.newArrayList();

    /**
     * The raw data that was used in the attempted import.
     */
    private final Multimap<String, String> importData;

    /**
     * The records into which the data was imported.
     */
    private final Set<Long> records;

    /**
     * Construct a new instance.
     *
     * @param importData
     */
    private ImportResult(Multimap<String, String> importData, Set<Long> records) {
        this.importData = importData;
        this.records = records;
    }

    /**
     * Add an indication that an error has occurred, described by
     * {@code message}.
     *
     * @param message
     */
    protected void addError(String message) {
        errors.add(message);
    }

    /**
     * Return the number of errors that occurred during the import.
     *
     * @return the error count
     */
    public int getErrorCount() {
        return errors.size();
    }

    /**
     * Return the raw data that was used in the import.
     *
     * @return the import data
     */
    public Multimap<String, String> getImportData() {
        return importData;
    }

    /**
     * Return the records into which the data was imported.
     *
     * @return the records
     */
    public Set<Long> getRecords() {
        return records;
    }

    /**
     * Return {@code true} if the import encountered errors.
     *
     * @return {@code true} if there were errors
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

}

