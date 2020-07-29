/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.settings.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.sling.settings.SlingSettingsService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.osgi.framework.BundleContext;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.Converters;

@RunWith(MockitoJUnitRunner.class)
public class RunModeImplTest {

    private SlingSettingsServiceImpl.Configuration configuration;

    @Mock
    BundleContext mockBundleContext;

    private Map<String, File> files = new HashMap<String, File>();

    private String runModes;
    private String options;
    private String installOptions;

    @Before
    public void before() {
        Converter c = Converters.standardConverter();
        // use standard configuration
        configuration = c.convert(new HashMap<String, Object>()).to(SlingSettingsServiceImpl.Configuration.class);
        runModes = null;
        options = null;
        installOptions = null;
        Mockito.when(mockBundleContext.getDataFile(Mockito.anyString())).then(new Answer<File>() {

            @Override
            public File answer(InvocationOnMock invocation) throws Throwable {
                String filename = invocation.getArgumentAt(0, String.class);
                File f = files.get(filename);
                if ( f == null ) {
                    try {
                        f = File.createTempFile(filename, "id");
                        f.delete();
                        files.put(filename, f);
                    } catch (IOException ioe) {
                        throw new RuntimeException(ioe);
                    }
                }
                return f;
            }
        });
 
        Mockito.when(mockBundleContext.getProperty(Mockito.anyString())).then(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                String key = invocation.getArgumentAt(0, String.class);
                if ( key.equals(SlingSettingsService.RUN_MODES_PROPERTY) ) {
                    return runModes;
                } else if ( key.equals(SlingSettingsService.RUN_MODE_OPTIONS) ) {
                    return options;
                } else if ( key.equals(SlingSettingsService.RUN_MODE_INSTALL_OPTIONS) ) {
                    return installOptions;
                }
                return null;
            }
         });
    }

    private void assertParse(String str, String[] expected) {
        final SlingSettingsService rm = createSlingSettingsService(str, null, null);
        final Set<String> modes = rm.getRunModes();

        Set<String> expectedSet = new HashSet<String>(expected.length);
        for (String expectedEntry : expected) {
            expectedSet.add(expectedEntry);
        }

        Assert.assertEquals("Parsed runModes match for '" + str + "'", expectedSet, modes);
    }

    @org.junit.Test
    public void testParseRunModes() {
        assertParse(null, new String[0]);
        assertParse("", new String[0]);
        assertParse(" foo \t", new String[] { "foo" });
        assertParse(" foo \t,  bar\n", new String[] { "foo", "bar" });
    }

    private void assertActive(SlingSettingsService s, boolean active, String... modes) {
        for (String mode : modes) {
            if (active) {
                assertTrue(mode + " should be active", s.getRunModes().contains(mode));
            } else {
                assertFalse(mode + " should NOT be active", s.getRunModes().contains(mode));
            }
        }
    }

    @org.junit.Test
    public void testMatchesNotEmpty() {
        final SlingSettingsService rm = createSlingSettingsService("foo,bar", null, null);
        assertActive(rm, true, "foo", "bar");
        assertActive(rm, false, "wiz", "bah", "");
    }

    @org.junit.Test
    public void testOptions() {
        final SlingSettingsService rm = createSlingSettingsService("foo,bar", "a,b,c|d,e,f", null);
        assertActive(rm, true, "foo", "bar", "a", "d");
        assertActive(rm, false, "b", "c", "e", "f");
    }

    @org.junit.Test
    public void testEmptyRunModesWithOptions() {
        final SlingSettingsService rm = createSlingSettingsService("", "a,b,c|d,e,f", null);
        assertActive(rm, true, "a", "d");
        assertActive(rm, false, "b", "c", "e", "f");
    }

    @org.junit.Test
    public void testOptionsSelected() {
        final SlingSettingsService rm = createSlingSettingsService("foo,bar,c,e", "a,b,c|d,e,f", null);
        assertActive(rm, true, "foo", "bar", "c", "e");
        assertActive(rm, false, "a", "b", "d", "f");
    }

    @org.junit.Test
    public void testOptionsMultipleSelected() {
        final SlingSettingsService rm = createSlingSettingsService("foo,bar,c,e,f,a", "a,b,c|d,e,f", null);
        assertActive(rm, true, "foo", "bar", "a", "e");
        assertActive(rm, false, "b", "c", "d", "f");
    }

    @org.junit.Test
    public void testOptionsMultipleSelected2() {
        final SlingSettingsService rm = createSlingSettingsService("foo,bar,c,f,a,d", "a,b,c|d,e,f", null);
        assertActive(rm, true, "foo", "bar", "a", "d");
        assertActive(rm, false, "b", "c", "e", "f");
    }

    @org.junit.Test
    public void testInstallOptions() {
        final SlingSettingsService rm = createSlingSettingsService("foo,bar", null, "a,b,c|d,e,f");
        assertActive(rm, true, "foo", "bar", "a", "d");
        assertActive(rm, false, "b", "c", "e", "f");
    }

    @org.junit.Test
    public void testInstallOptionsSelected() {
        final SlingSettingsService rm = createSlingSettingsService("foo,bar,c,e", null, "a,b,c|d,e,f");
        assertActive(rm, true, "foo", "bar", "c", "e");
        assertActive(rm, false, "a", "b", "d", "f");
    }

    @org.junit.Test
    public void testInstallOptionsMultipleSelected() {
        final SlingSettingsService rm = createSlingSettingsService("foo,bar,c,e,f,a", null, "a,b,c|d,e,f");
        assertActive(rm, true, "foo", "bar", "a", "e");
        assertActive(rm, false, "b", "c", "d", "f");
    }

    @org.junit.Test
    public void testInstallOptionsMultipleSelected2() {
        final SlingSettingsService rm = createSlingSettingsService("foo,bar,c,d,f,a", null, "a,b,c|d,e,f");
        assertActive(rm, true, "foo", "bar", "a", "d");
        assertActive(rm, false, "b", "c", "e", "f");
    }

    @org.junit.Test
    public void testInstallOptionsRestart() {
        {
            // create first context to simulate install
            final SlingSettingsService rm = createSlingSettingsService("foo,bar,c,e,f,a", null, "a,b,c|d,e,f");
            assertActive(rm, true, "foo", "bar", "a", "e");
            assertActive(rm, false, "b", "c", "d", "f");
        }

        // simulate restart with different run modes: new ones that are
        // mentioned in the .options properties are ignored
        {
            SlingSettingsService rm = createSlingSettingsService("foo,bar,c,e,f,a", null, "a,b,c|d,e,f");
            assertActive(rm, true, "foo", "bar", "a", "e");
            assertActive(rm, false, "b", "c", "d", "f");
            rm = createSlingSettingsService("foo,doo,a,b,c,d,e,f,waa", null, "a,b,c|d,e,f");
            assertActive(rm, true, "foo", "doo", "a", "e", "waa");
            assertActive(rm, false, "bar", "b", "c", "d", "f");
        }
    }

    private SlingSettingsService createSlingSettingsService(String runModes, String options, String installOptions) {
        this.runModes = runModes;
        this.options = options;
        this.installOptions = installOptions;
        return new SlingSettingsServiceImpl(configuration, mockBundleContext);
    }
}