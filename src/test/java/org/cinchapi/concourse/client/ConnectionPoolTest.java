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
package org.cinchapi.concourse.client;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import junit.framework.Assert;

import org.cinchapi.concourse.Concourse;
import org.cinchapi.concourse.config.ConcourseClientPreferences;
import org.cinchapi.concourse.time.Time;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import com.google.common.collect.Lists;

/**
 * Unit tests for {@link ConnectionPool}.
 * 
 * @author jnelson
 */
public class ConnectionPoolTest {

    @Rule
    public TestWatcher watcher = new TestWatcher() {

        @Override
        protected void finished(Description description) {       
            try {
                connections.close();
                connections = null;
                Files.delete(Paths.get(prefs));
            }
            catch (Exception e) {}
            prefs = null;
        }

        @Override
        protected void starting(Description description) {
            prefs = Time.now() + "";
            connections = getConnectionPool();
        }

    };

    private ConnectionPool connections = null;
    private String prefs = null;

    private static final int POOL_SIZE = 3;

    @Test
    public void testHasAvailableConnection() {
        Assert.assertTrue(connections.hasAvailableConnection());
    }

    @Test
    public void testNotHasAvailableConnectionWhenAllInUse() {
        List<Concourse> toReturn = Lists.newArrayList();
        for (int i = 0; i < POOL_SIZE; i++) {
            toReturn.add(connections.request());
        }
        Assert.assertFalse(connections.hasAvailableConnection());
        for (Concourse concourse : toReturn) {
            connections.restore(concourse);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCannotReturnConnectionNotRequestedFromPool() {
        ConcourseClientPreferences cp = ConcourseClientPreferences.load(prefs);
        connections.restore(Concourse.connect(cp.getHost(), cp.getPort(),
                cp.getUsername(), new String(cp.getPassword())));
    }

    /**
     * Retrun a {@link ConnectionPool} to use in a unit test.
     * 
     * @return the ConnectionPool
     */
    private final ConnectionPool getConnectionPool() {
        return ConnectionPool.newConnectionPool(prefs, POOL_SIZE); // get a pool
                                                                   // using the
                                                                   // default
                                                                   // connection
                                                                   // info,
                                                                   // but in the
                                                                   // future use
                                                                   // the test
                                                                   // framework
                                                                   // and
                                                                   // get the
                                                                   // correct
                                                                   // pool
    }

}
