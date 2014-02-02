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

import org.cinchapi.concourse.importer.CsvImporter;
import org.cinchapi.concourse.importer.LineImporter;

/**
 * A CLI that imports CSV files into Concourse.
 * 
 * @author jnelson
 */
public class CsvImportCli extends LineImportCli {

    /**
     * Run the program
     * 
     * @param args
     */
    public static void main(String... args) {
        CsvImportCli cli = new CsvImportCli(args);
        cli.run();
    }

    /**
     * Construct a new instance.
     * @param args
     */
    public CsvImportCli(String... args) {
        super(args);
    }

    @Override
    protected LineImporter importer() {
        return CsvImporter.withConnectionInfo(options.host, options.port,
                options.username, options.password);
    }

}
