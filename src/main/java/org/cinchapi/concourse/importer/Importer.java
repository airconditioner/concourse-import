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

import java.util.Collection;

/**
 * An {@link Importer} is a service that has the ability to go through a file
 * and import contents into {@link Concourse}.
 * 
 * @author jnelson
 */
public interface Importer {

    /**
     * Import the data contained in {@code file} into {@link Concourse}.
     * 
     * @param file
     * @return a collection of {@link ImportResult} objects that describes the
     *         records created/affected from the import and whether any errors
     *         occurred
     */
    public Collection<ImportResult> importFile(String file);

    /**
     * Import the data contained in {@code file} into {@link Concourse}.
     * <p>
     * <strong>Note</strong> that if {@code resolveKey} is specified, an attempt
     * will be made to add the data in from each group into the existing records
     * that are found using {@code resolveKey} and its corresponding value in
     * the group.
     * </p>
     * 
     * @param file
     * @param resolveKey
     * @return a collection of {@link ImportResult} objects that describes the
     *         records created/affected from the import and whether any errors
     *         occurred.
     */
    public Collection<ImportResult> importFile(String file, String resolveKey);

}
