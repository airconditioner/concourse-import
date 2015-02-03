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

package org.cinchapi.concourse.importer.cli;

import org.cinchapi.concourse.test.ClientServerTest;
import org.junit.Test;

/**
 * Test cases for {@link GeneralCsvImportCli}.
 * 
 * @author hmitchell
 *
 */
public class GeneralCsvImportCliTest extends ClientServerTest {

    /**
     * Test that a {@link GeneralCsvImportCli} command line interface can accept
     * arguments and import a directory of files without any exceptions.
     */
    @Test
    public void testImportDirectory() throws Exception {
        getImportCli("admin").run();
    }

    /**
     * Return a {@link GeneralCsvImportCli} created with default arguments and
     * {@link pathToData}.
     * 
     * @param password
     * @return the import cli
     */
    public GeneralCsvImportCli getImportCli(String password) {
        return new GeneralCsvImportCli("--password", password, "-p",
                String.valueOf(server.getClientPort()), "-d", pathToData());
    }

    /**
     * Path to data to import for test cases.
     * 
     * @return string value of path
     */
    protected String pathToData() {
        return "bin/../src/test/resources";
    }

    @Override
    protected String getServerVersion() {
        return "0.3.4";
    }

    /**
     * {@link GeneralCsvImportCli} used for test cases.
     */
    protected GeneralCsvImportCli importCli;

}
