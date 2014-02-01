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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.cinchapi.concourse.cli.CommandLineInterface;
import org.cinchapi.concourse.cli.Options;

import com.beust.jcommander.Parameter;
import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

/**
 * An abstract implementation for a CLI that imports data. This class handles
 * the boilerplate logic of finding all the files to import. The subclass is
 * responsible for defining its own importer to handle each file.
 * <p>
 * The subclass may also define a {@link #whitelist()} to customize its import.
 * </p>
 * 
 * @author jnelson
 */
public abstract class AbstractImportCli extends CommandLineInterface {

    /**
     * The list of files or file extensions that are included in the import.
     * This list is populated by the subclass implementation of the
     * {@link #whitelist()} method.
     */
    private final List<String> whitelist = whitelist();

    /**
     * Construct a new instance.
     * 
     * @param options
     * @param args
     */
    protected AbstractImportCli(ImportOptions options, String[] args) {
        super(options, args);
    }

    /**
     * Import {@code file}.
     * 
     * @param file
     */
    protected abstract void doImport(String file); // the subclass is
                                                   // responsible for defining
                                                   // its own importer

    @Override
    protected final void doTask() {
        String data = ((ImportOptions) options).data;
        List<String> files = scan(Paths.get(data));
        Stopwatch watch = Stopwatch.createStarted();
        for (String file : files) {
            // TODO multithread imports?
            doImport(file);
        }
        watch.stop();
        TimeUnit unit = TimeUnit.SECONDS;
        System.out.println(MessageFormat.format("Finished import in {0}, {1}",
                watch.elapsed(unit), unit));
    }

    /**
     * Recursively scan and collect all the files in the directory defined by
     * {@code path}.
     * 
     * @param path
     * @return the list of files in the directory
     */
    protected List<String> scan(Path path) {
        try {
            List<String> files = Lists.newArrayList();
            if(Files.isDirectory(path)) {
                Iterator<Path> it = Files.newDirectoryStream(path).iterator();
                while (it.hasNext()) {
                    files.addAll(scan(path));
                }
            }
            else {
                String name = path.toString();
                if(whitelist == null) {
                    files.add(path.toString());
                }
                else {
                    for (String string : whitelist) {
                        if(name.endsWith(string)) {
                            files.add(path.toString());
                            break;
                        }
                    }
                }

            }
            return files;
        }
        catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    /**
     * Return a list of files or file extensions that should be included in the
     * import. By default, an attempt is made to import every file that is
     * encountered. But, the subclass can specify files or file extensions to
     * whitelist in this method. Any file that ends with an element of the
     * whitelist will be included in the import.
     * 
     * @return the whitelist
     */
    protected List<String> whitelist() {
        return null;
    }

    /**
     * Import specific {@link Options}.
     * 
     * @author jnelson
     */
    protected static class ImportOptions extends Options {

        @Parameter(names = { "-d", "--data" }, description = "The path to the file or directory to import", required = true)
        public String data;

    }

}
