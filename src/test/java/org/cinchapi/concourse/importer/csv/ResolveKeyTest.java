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
package org.cinchapi.concourse.importer.csv;

import java.util.Collection;
import java.util.Set;

import junit.framework.Assert;

import org.cinchapi.concourse.importer.ImportResult;
import org.cinchapi.concourse.thrift.Operator;
import org.cinchapi.concourse.util.Convert;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.Iterables;

/**
 * 
 * 
 * @author jnelson
 */
public class ResolveKeyTest extends CsvImportBaseTest {

    @Override
    @Test
    @Ignore
    public void testImport() {}

    @Override
    protected String getImportFile() {
        return null;
    }

    @Test
    public void testResolveKey() {
        String file0 = this.getClass().getResource("/resolve_key_0.csv")
                .getFile();
        String file1 = this.getClass().getResource("/resolve_key_1.csv")
                .getFile();
        String resolveKey = "ipeds_id";
        importer.importFile(file0, resolveKey);
        Collection<ImportResult> results = importer.importFile(file1,
                resolveKey);
        for (ImportResult result : results) {
            Object value = Convert.stringToJava(Iterables.getOnlyElement(result
                    .getImportData().get(resolveKey)));
            Set<Long> records = client.find(resolveKey, Operator.EQUALS, value);
            System.out.println("here");
            Assert.assertEquals(1, records.size());
        }

    }

}
