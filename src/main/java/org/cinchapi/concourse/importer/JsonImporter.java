/*
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 Jeff Nelson, Cinchapi Software Collective
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

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Collection;

import javax.annotation.Nullable;

import org.cinchapi.concourse.Concourse;
import org.cinchapi.concourse.util.FileUtility;

import com.google.common.base.Throwables;

/**
 * <p>
 * An {@link Importer} that can handle files in Json syntax. It is assumed in
 * this Json format that single quotes are not accepted for enclosing keys or
 * values, but they can be used as characters in a string.
 * </p>
 * 
 * @author hyin
 */
public class JsonImporter extends AbstractImporter {

    /**
     * Construct a new instance with a Concourse instance.
     * 
     * @param concourse
     */
    protected JsonImporter(Concourse concourse) {
        super(concourse);
    }

    @Override
    /**
     *  Import the Json objects by parsing each Json object in
     *  the file and calling the Concourse insert method on it.
     * 
     *  @param file
     *  @param resolveKey
     *  @return the results of the import
     */
    protected Collection<ImportResult> handleFileImport(String file,
            @Nullable String resolveKey) {
        try {
            FileReader fr = new FileReader(FileUtility.expandPath(file));
            BufferedReader reader = new BufferedReader(fr);

            StringBuilder jsonObj = new StringBuilder();

            // Boolean tags to indicate start of
            // Json object and String
            boolean start = false;
            boolean quote = false;
            char doubleQuote = '\"';
            
            int c;
            while ((c = reader.read()) != -1) {
                c = (char) c;
                if (c == doubleQuote && start == true) {
                    quote = !quote;
                    jsonObj.append(c);
                }
                // Brackets in double quotes should count as 
                // character in String, not start/end of object
                else if (c == '{' && start == false && !quote) {
                    start = true;
                    jsonObj.append(c);
                } 
                else if (c == '}' && start == true && !quote) {
                    start = false;
                    jsonObj.append(c);
                    concourse.insert((jsonObj.toString()));
                    jsonObj = new StringBuilder();
                } 
                else if (start == true) {
                    jsonObj.append(c);
                }
            }
            reader.close();
            
        }
        catch (Exception e) {
            throw Throwables.propagate(e);
        }
        return null;
    }

}
