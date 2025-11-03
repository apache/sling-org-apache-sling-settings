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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.sling.settings.SlingSettingsService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.Converters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SlingSettingsServiceImplTest {

    private static final String SLING_ID_FILE_NAME = "sling.id.file";

    private static final String OPTIONS_FILE_NAME = "sling.options.file";

    private File slingIdFile = null;

    private File optionsFile = null;

    private SlingSettingsServiceImpl.Configuration configuration;

    @Before
    public void before() throws IOException {
        slingIdFile = Files.createTempFile(SLING_ID_FILE_NAME, "").toFile();
        optionsFile = Files.createTempFile(OPTIONS_FILE_NAME, "").toFile();
        Converter c = Converters.standardConverter();
        // use standard configuration
        configuration = c.convert(new HashMap<String, Object>()).to(SlingSettingsServiceImpl.Configuration.class);
    }

    @After
    public void after() throws IOException {
        if (slingIdFile != null) {
            slingIdFile.delete();
            slingIdFile = null;
        }
        if (optionsFile != null) {
            optionsFile.delete();
            optionsFile = null;
        }
    }

    @Test
    public void testGetSlingIdCreating() throws IOException {
        final SlingSettingsService slingSettingsService = createSlingSettingsService(slingIdFile, optionsFile);

        final String slingId = slingSettingsService.getSlingId();
        assertNotNull(slingId);
    }

    @Test
    public void testGetSlingIdExisting() throws IOException {
        final String expected = SlingIdUtil.createSlingId();
        SlingIdUtil.writeSlingId(slingIdFile, expected);
        final SlingSettingsService slingSettingsService = createSlingSettingsService(slingIdFile, optionsFile);

        final String slingId = slingSettingsService.getSlingId();
        assertNotNull(slingId);
        assertEquals(expected, slingId);
    }

    @Test
    public void testGetSlingIdFromTooLargeData() throws IOException {
        final String expected = SlingIdUtil.createSlingId();
        final String data = expected + RandomStringUtils.randomAscii(1024 * 1024); // 1MB long random String
        SlingIdUtil.writeSlingId(slingIdFile, data);
        final SlingSettingsService slingSettingsService = createSlingSettingsService(slingIdFile, optionsFile);

        final String slingId = slingSettingsService.getSlingId();
        assertNotNull(slingId);
        assertEquals(expected, slingId);
    }

    @Test
    public void testGetSlingIdFromTooShortData() throws IOException {
        final String data = RandomStringUtils.randomAscii(8); // 8 byte long random String
        SlingIdUtil.writeSlingId(slingIdFile, data);
        final SlingSettingsService slingSettingsService = createSlingSettingsService(slingIdFile, optionsFile);

        final String slingId = slingSettingsService.getSlingId();
        assertNotNull(slingId);
    }

    @Test
    public void testGetBestRunModeMatchCountFromSpec() {
        Assert.assertEquals(
                0,
                SlingSettingsServiceImpl.getBestRunModeMatchCountFromSpec(
                        "test1.test2,-test3.test4", Collections.singleton("test5")));
        Assert.assertEquals(
                0,
                SlingSettingsServiceImpl.getBestRunModeMatchCountFromSpec(
                        "test1.test2,-test3.test4", Stream.of("test1", "test3").collect(Collectors.toSet())));
        Assert.assertEquals(
                0,
                SlingSettingsServiceImpl.getBestRunModeMatchCountFromSpec(
                        "test1.test2,-test3.test4", Stream.of("test2", "test3").collect(Collectors.toSet())));
        Assert.assertEquals(
                2,
                SlingSettingsServiceImpl.getBestRunModeMatchCountFromSpec(
                        "test1.test2,-test3.test4", Stream.of("test1", "test2").collect(Collectors.toSet())));
        Assert.assertEquals(
                2,
                SlingSettingsServiceImpl.getBestRunModeMatchCountFromSpec(
                        "test1.test2,-test3.test4", Stream.of("test2", "test4").collect(Collectors.toSet())));
        Assert.assertEquals(
                3,
                SlingSettingsServiceImpl.getBestRunModeMatchCountFromSpec(
                        "test1.test2,-test3.test4,test5.test6.test7",
                        Stream.of("test1", "test2", "test4", "test5", "test6", "test7")
                                .collect(Collectors.toSet())));
    }

    private SlingSettingsService createSlingSettingsService(final File slingIdFile, final File optionsFile)
            throws IOException {
        BundleContext context = mock(BundleContext.class);
        when(context.getDataFile(SLING_ID_FILE_NAME)).thenReturn(slingIdFile);
        when(context.getDataFile(OPTIONS_FILE_NAME)).thenReturn(optionsFile);
        // write options
        final List<SlingSettingsServiceImpl.Options> options = new ArrayList<SlingSettingsServiceImpl.Options>();
        try (FileOutputStream fos = new FileOutputStream(optionsFile);
                ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(options);
        } catch (final IOException ioe) {
            throw new RuntimeException("Unable to write to options data file.", ioe);
        }
        return new SlingSettingsServiceImpl(configuration, context);
    }
}
