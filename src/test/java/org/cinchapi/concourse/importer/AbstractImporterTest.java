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

import org.cinchapi.concourse.Link;
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

    // NOTE: This class contains unit tests for static methods

    @Test
    public void testTransformValueToResolvableLink() {
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
    public void testResolvableLinkKeyAndValueRegexWithNumbers() {
        String key = RAW_RESOLVABLE_LINK_SYMBOL_PREPEND
                + Random.getNumber().toString()
                + RAW_RESOLVABLE_LINK_SYMBOL_APPEND;
        String string = key + Random.getNumber().toString() + key;
        Assert.assertTrue(string.matches(MessageFormat.format("{0}{1}{0}",
                MessageFormat.format("{0}{1}{2}",
                        RAW_RESOLVABLE_LINK_SYMBOL_PREPEND, ".+",
                        RAW_RESOLVABLE_LINK_SYMBOL_APPEND), ".+")));
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
    public void testConvertResolvableLink() {
        String key = Random.getString().replace(" ", "");
        String value = Random.getObject().toString().replace(" ", "");
        ResolvableLink link = (ResolvableLink) LineImporter
                .convert(LineImporter
                        .transformValueToResolvableLink(key, value));
        Assert.assertEquals(link.key, key);
        Assert.assertEquals(link.value, AbstractImporter.convert(value));
    }

    @Test
    public void testConvertForcedStringSingleQuotes() {
        // A value that is wrapped in single (') or double (") quotes must
        // always be converted to a string
        Object object = Random.getObject();
        String value = MessageFormat
                .format("{0}{1}{0}", "'", object.toString());
        Assert.assertEquals(AbstractImporter.convert(value), object.toString());
    }

    @Test
    public void testConvertForcedStringDoubleQuotes() {
        // A value that is wrapped in single (') or double (") quotes must
        // always be converted to a string
        Object object = Random.getObject();
        String value = MessageFormat.format("{0}{1}{0}", "\"",
                object.toString());
        Assert.assertEquals(AbstractImporter.convert(value), object.toString());
    }

    @Test
    public void testConvertLinkFromLongValue() {
        // A int/long that is wrapped between two at (@) symbols must always
        // convert to a Link
        Number number = Random.getLong();
        String value = MessageFormat
                .format("{0}{1}{0}", "@", number.toString()); // must use
                                                              // number.toString()
                                                              // so comma
                                                              // separators are
                                                              // not added to
                                                              // the output
        Link link = (Link) AbstractImporter.convert(value);
        Assert.assertEquals(number.longValue(), link.longValue());
    }

    @Test
    public void testConvertLinkFromIntValue() {
        // A int/long that is wrapped between two at (@) symbols must always
        // convert to a Link
        Number number = Random.getInt();
        String value = MessageFormat
                .format("{0}{1}{0}", "@", number.toString()); // must use
                                                              // number.toString()
                                                              // so comma
                                                              // separators are
                                                              // not added to
                                                              // the output
        Link link = (Link) AbstractImporter.convert(value);
        Assert.assertEquals(number.intValue(), link.intValue());
    }

    @Test
    public void testCannotConvertLinkFromFloatValue() {
        Number number = Random.getFloat();
        String value = MessageFormat
                .format("{0}{1}{0}", "@", number.toString());
        Assert.assertFalse(AbstractImporter.convert(value) instanceof Link);
    }

    @Test
    public void testCannotConvertLinkFromDoubleValue() {
        Number number = Random.getDouble();
        String value = MessageFormat
                .format("{0}{1}{0}", "@", number.toString());
        Assert.assertFalse(AbstractImporter.convert(value) instanceof Link);
    }

    @Test
    public void testCannotConvertLinkFromBooleanValue() {
        Boolean number = Random.getBoolean();
        String value = MessageFormat
                .format("{0}{1}{0}", "@", number.toString());
        Assert.assertFalse(AbstractImporter.convert(value) instanceof Link);
    }

    @Test
    public void testCannotConvertLinkFromStringValue() {
        String number = Random.getString();
        String value = MessageFormat
                .format("{0}{1}{0}", "@", number.toString());
        Assert.assertFalse(AbstractImporter.convert(value) instanceof Link);
    }

    @Test
    public void testConvertBoolean() {
        Boolean bool = Random.getBoolean();
        String boolString = scrambleCase(bool.toString());
        Assert.assertEquals(bool, AbstractImporter.convert(boolString));  
    }
    
    @Test
    public void testConvertInteger(){
        Number number = Random.getInt();
        String string = number.toString();
        Assert.assertEquals(number, AbstractImporter.convert(string));
    }
    
    @Test
    public void testConvertLong(){
        Number number = null;
        while(number == null || (Long) number <= Integer.MAX_VALUE){
            number = Random.getLong();
        }
        String string = number.toString();
        Assert.assertEquals(number, AbstractImporter.convert(string));
    }
    
    @Test
    public void testConvertFloat(){
        Number number = Random.getFloat();
        String string = number.toString();
        Assert.assertEquals(number, AbstractImporter.convert(string));
    }
    
    @Test
    public void testConvertDouble(){
        Number number = Random.getDouble();
        String string = number.toString() + "D";
        Assert.assertEquals(number, AbstractImporter.convert(string));
    }

    /**
     * Randomly flip the case of all the characters in {@code string}.
     * 
     * @param string
     * @return the case scrambled string
     */
    private String scrambleCase(String string) {
        char[] chars = string.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if(Random.getInt() % 2 == 0) {
                c = Character.toLowerCase(c);
            }
            else {
                c = Character.toUpperCase(c);
            }
            chars[i] = c;
        }
        return new String(chars);
    }

}
