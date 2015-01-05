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

package org.cinchapi.concourse.util;

import java.nio.file.FileSystems;
import java.nio.file.Path;

 
/**
 * Simple Utility providing tools for working with the file system and paths.
 * 
 * @author jnelson
 */
public class FileUtility {
 
    /**
     * The user's home directory, which is used to expand path names with "~"
     * (tilde).
     */
    private static String USER_HOME = System.getProperty("user.home");
 
    /**
     * The working directory from which the current JVM process was launched.
     */
    private static String WORKING_DIRECTORY = System.getProperty("user.dir");
 
    /**
     * The base path that is used to resolve and normalize other relative paths.
     */
    private static Path BASE_PATH = FileSystems.getDefault().getPath(
            WORKING_DIRECTORY);
 
    /**
     * Expand the given {@code path} so that it contains completely normalized
     * components (e.g. ".", "..", and "~" are resolved to the correct absolute
     * paths).
     * 
     * @param path
     * @return the expanded path
     */
    public static String expandPath(String path) {
        path = path.replaceAll("~", USER_HOME);
        path = path.replaceAll("\\$HOME", USER_HOME);
        return BASE_PATH.resolve(path).normalize().toString();
    }
    
    /**
     * Returns the value of {@link USER_HOME} directory.
     * @return
     */
    public static String getUserHome (){
    	return USER_HOME;
    }
    
    /**
     * Returns the value of {@link BASE_PATH} directory.
     * @return
     */
    public static Path getBasePath(){
    	return BASE_PATH;
    }
    
    /**
     * Returns the value of {@link WORKING_DIRECTORY} directory.
     * @return
     */
    public static String getWorkingDirectory (){
    	return WORKING_DIRECTORY;
    }
    
}
