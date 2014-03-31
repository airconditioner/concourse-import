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
package org.cinchapi.concourse.importer;

import java.util.Arrays;
import junit.framework.Assert;

import org.junit.Test;

/**
 * Unit tests for {@link GeneralCsvImporter}.
 * 
 * @author jnelson
 */
public class GeneralCsvImporterTest {

    @Test
    public void testSplitStringByDelimterAndRespectQuotes() {
        String string = "Sachin,,M,\"Maths,Science,English\",Need to improve in these subjects.";
        String[] splitted = GeneralCsvImporter
                .splitStringByDelimterAndRespectQuotes(string, ",");
        Assert.assertEquals(
                "[Sachin, , M, \"Maths,Science,English\", Need to improve in these subjects.]",
                Arrays.toString(splitted));
    }

    @Test
    public void testSplitStringByDelimeterWithTrailiningSpaceAndRespectQuotes() {
        String string = "\"New Leaf, Same Page \"";
        String[] splitted = GeneralCsvImporter
                .splitStringByDelimterAndRespectQuotes(string, ",");
        Assert.assertEquals("[\"New Leaf, Same Page \"]",
                Arrays.toString(splitted));
    }

}
