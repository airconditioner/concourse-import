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

import org.cinchapi.concourse.cli.Options;
import org.cinchapi.concourse.importer.CsvImporter;

import com.beust.jcommander.Parameter;

/**
 * A CLI that imports CSV files into Concourse.
 * 
 * @author jnelson
 */
public class CsvImportCli extends AbstractImportCli {

    /**
     * Run the program
     * 
     * @param args
     */
    public static void main(String... args) {
        CsvImportCli cli = new CsvImportCli(new CsvImportOptions(), args);
        cli.run();
    }

    /**
     * The importer that is used to bring the data in each file into Concourse.
     */
    private final CsvImporter importer;

    /**
     * Construct a new instance.
     * 
     * @param options
     * @param args
     */
    protected CsvImportCli(CsvImportOptions options, String[] args) {
        super(options, args);
        this.importer = CsvImporter.withConnectionInfo(options.host,
                options.port, options.username, options.password); // must call
                                                                   // after
                                                                   // super
                                                                   // constructors
                                                                   // so all
                                                                   // creds are
                                                                   // initialized
    }

    @Override
    protected void doImport(String file) {
        importer.importFile(file, ((CsvImportOptions) options).resolveKey);
    }

    /**
     * CsvImport specific {@link Options}.
     * 
     * @author jnelson
     */
    protected static class CsvImportOptions extends ImportOptions {

        @Parameter(names = { "-r", "--resolveKey" }, description = "The key to use when resolving data into existing records")
        public String resolveKey = null;

    }

}
