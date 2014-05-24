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

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import junit.framework.Assert;

import org.cinchapi.concourse.importer.GeneralCsvImporter;
import org.cinchapi.concourse.importer.ImportResult;
import org.cinchapi.concourse.test.ClientServerTest;
import org.cinchapi.concourse.test.Variables;
import org.cinchapi.concourse.thrift.Operator;
import org.cinchapi.concourse.util.Convert;
import org.cinchapi.concourse.util.Convert.ResolvableLink;
import org.cinchapi.concourse.util.Resources;
import org.junit.Test;

import com.google.common.base.Strings;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

/**
 * The base test for those that verify the integrity of the import framework for
 * a given file.
 * 
 * @author jnelson
 */
public abstract class CsvImportBaseTest extends ClientServerTest {

    protected GeneralCsvImporter importer;

    @Override
    protected void afterEachTest() {
        importer = null;
    }

    @Override
    protected void beforeEachTest() {
        importer = getImporter();
    }

    @Test
    public void testImport() throws IOException {
        Collection<ImportResult> results = importer.importFile(Resources.get(
                "/" + getImportFile()).getFile());
        for (ImportResult result : results) {
            // Verify no errors
            Assert.assertEquals(0, result.getErrorCount());

            // Verify that fetch reads return all the appropriate values
            Multimap<String, String> data = result.getImportData();
            for (long record : result.getRecords()) {
                for (String key : data.keySet()) {
                    for (String value : data.get(key)) {
                        if(!Strings.isNullOrEmpty(value)) {
                            Object expected = Convert.stringToJava(value);
                            if(!(expected instanceof ResolvableLink)) {
                                Variables.register("key", key);
                                Variables.register("expected", expected);
                                Variables.register("record", record);
                                Set<Object> stored = client.fetch(key, record);
                                int count = 0;
                                for (Object obj : stored) {
                                    Variables.register("stored_" + count, obj);
                                }
                                Assert.assertTrue(stored.contains(expected));
                            }
                        }
                    }
                }
            }

            // Verify that find queries return all the appropriate records
            for (String key : data.keySet()) {
                for (String value : data.get(key)) {
                    if(!Strings.isNullOrEmpty(value)) {
                        Object stored = Convert.stringToJava(value);
                        if(!(stored instanceof ResolvableLink)) {
                            Assert.assertEquals(
                                    result.getRecords().size(),
                                    Sets.intersection(
                                            result.getRecords(),
                                            client.find(key, Operator.EQUALS,
                                                    stored)).size());
                        }
                    }
                }
            }
        }
    }

    /**
     * Return the path to the file to import.
     * 
     * @return the import file
     */
    protected abstract String getImportFile();

    /**
     * Return the importer to use in the tests.
     * 
     * @return the importer
     */
    protected GeneralCsvImporter getImporter() {
        return GeneralCsvImporter.withConnectionInfo("localhost",
                server.getClientPort(), "admin", "admin");
    }

    @Override
    protected String getServerVersion() {
        return "0.3.4";
    }

}
