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

import java.text.MessageFormat;

import org.cinchapi.concourse.importer.AbstractImporter.ResolvableLink;
import org.cinchapi.concourse.util.Random;
import org.junit.Assert;
import org.junit.Test;
import static org.cinchapi.concourse.importer.AbstractImporter.RAW_RESOLVABLE_LINK_SYMBOL_APPEND;
import static org.cinchapi.concourse.importer.AbstractImporter.RAW_RESOLVABLE_LINK_SYMBOL_PREPEND;

/**
 * Unit tests for {@link AbstractImporter}.
 * 
 * @author jnelson
 */
public class AbstractImporterTest {

    @Test
    public void testTransformValueToResolvableLink() { // test of static method
        String key = Random.getString();
        String value = Random.getObject().toString();
        Assert.assertEquals(MessageFormat.format("{0}{1}{0}", MessageFormat
                .format("{0}{1}{2}", RAW_RESOLVABLE_LINK_SYMBOL_PREPEND, key,
                        RAW_RESOLVABLE_LINK_SYMBOL_APPEND), value),
                LineImporter.transformValueToResolvableLink(key, value));
    }

    @Test
    public void testResolvableLinkKeyRegexWithNumbers() {
        String string = RAW_RESOLVABLE_LINK_SYMBOL_PREPEND
                + Random.getNumber().toString()
                + RAW_RESOLVABLE_LINK_SYMBOL_APPEND;
        Assert.assertTrue(string.matches(MessageFormat.format("{0}{1}{2}",
                RAW_RESOLVABLE_LINK_SYMBOL_PREPEND, ".+",
                RAW_RESOLVABLE_LINK_SYMBOL_APPEND)));
    }
    
    @Test
    public void testResolvableLinkKeyAndValueRegexWithNumbers(){
        String key = RAW_RESOLVABLE_LINK_SYMBOL_PREPEND
                + Random.getNumber().toString()
                + RAW_RESOLVABLE_LINK_SYMBOL_APPEND;
        String string = key + Random.getNumber().toString() + key;
        Assert.assertTrue(string.matches(MessageFormat.format("{0}{1}{0}", MessageFormat
                .format("{0}{1}{2}", RAW_RESOLVABLE_LINK_SYMBOL_PREPEND,
                        ".+", RAW_RESOLVABLE_LINK_SYMBOL_APPEND),
                ".+")));
    }

    @Test
    public void testConvertResolvableLinkWithNumbers() {
        String key = Random.getNumber().toString();
        String value = Random.getNumber().toString();
        ResolvableLink link = (ResolvableLink) AbstractImporter
                .convert(LineImporter
                        .transformValueToResolvableLink(key, value));
        Assert.assertEquals(link.key, key);
        Assert.assertEquals(link.value, AbstractImporter.convert(value));
    }

    @Test
    public void testConvertResolvableLink() { // test of static method
        String key = Random.getString().replace(" ", "");
        String value = Random.getObject().toString().replace(" ", "");
        ResolvableLink link = (ResolvableLink) LineImporter
                .convert(LineImporter
                        .transformValueToResolvableLink(key, value));
        Assert.assertEquals(link.key, key);
        Assert.assertEquals(link.value, AbstractImporter.convert(value));
    }

}
