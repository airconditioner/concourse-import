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

import java.text.MessageFormat;
import java.util.Collection;

import org.cinchapi.concourse.cli.Options;
import org.cinchapi.concourse.importer.FileLineImporter;
import org.cinchapi.concourse.importer.ImportResult;

import com.beust.jcommander.Parameter;

/**
 * A CLI that can handle the features of a {@link FileLineImporter}.
 * 
 * @author jnelson
 */
public abstract class FileLineImportCli extends AbstractImportCli {

    /**
     * The importer that is used to bring the data in each file into Concourse.
     */
    private final FileLineImporter importer;

    /**
     * Construct a new instance.
     * 
     * @param args
     */
    public FileLineImportCli(String... args) {
        super(new LineImportOptions(), args);
        this.importer = importer(); // must call after super constructors so all
                                    // creds are initialized
    }

    @Override
    protected final void doImport(String file) {
        if(importer != null) {
            Collection<ImportResult> results = importer.importFile(file,
                    ((LineImportOptions) options).resolveKey);
            System.out.println(MessageFormat.format("Imported {0} lines",
                    results.size()));
        }
        else {
            System.err
                    .println("Cannot import file:  importer failed to initialize.");
        }
    }

    /**
     * The subclass should provide the importer that will be used by the CLI.
     * 
     * @return the importer
     */
    protected abstract FileLineImporter importer();

    /**
     * Line specific {@link Options}.
     * 
     * @author jnelson
     */
    protected static class LineImportOptions extends ImportOptions {

        @Parameter(names = { "-r", "--resolveKey" }, description = "The key to use when resolving data into existing records")
        public String resolveKey = null;

    }

}
