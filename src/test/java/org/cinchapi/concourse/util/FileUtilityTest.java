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

import org.junit.Assert;
import org.junit.Test;

/**
 * 
 * Unit tests for {@link FileUtility}.
 * @author hmitchell
 *
 */
public class FileUtilityTest {

	/**
	 * Test that relative paths with be resolved correctly.
	 */
	@Test 
    public void testExpandPath() {
		// Test paths starting with "." and containing ".."
        String path = "./bin/../src/resources";
        Assert.assertEquals("correct_path_1", FileUtility.getWorkingDirectory() + "/src/resources", FileUtility.expandPath(path));
        // Test paths with multiple ".."
        path = "bin/../bin/../src/resources";
        Assert.assertEquals("correct_path_2", FileUtility.getWorkingDirectory() + "/src/resources", FileUtility.expandPath(path));
        // Test Unix home tilde.
        path = "~";
        Assert.assertEquals("correct_path_3", FileUtility.getUserHome(), FileUtility.expandPath(path)); 
        // Test path with multiple consecutive forward slash.
        path = "./src/resources/////////////////";
        Assert.assertEquals("correct_path_4", FileUtility.getWorkingDirectory() + "/src/resources", FileUtility.expandPath(path)); 
        // Test path with multiple consecutive forward slash, followed by multiple "..".        
        path = "./src/resources/////////////////../..";
        Assert.assertEquals("correct_path_5", FileUtility.getWorkingDirectory(), FileUtility.expandPath(path));
        // Test path with $HOME.
        path = "$HOME";
        Assert.assertEquals("correct_path_6", FileUtility.getUserHome(), FileUtility.expandPath(path)); 
    }
}
