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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This is the basic implementation of the sling settings service. */
@Component
@Designate(ocd = SlingSettingsServiceImpl.Configuration.class)
@ServiceDescription("Apache Sling Settings Service")
public class SlingSettingsServiceImpl
        implements SlingSettingsService {

    /** The logger */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** The sling instance id. */
    private String slingId;

    /** The sling home */
    private String slingHome;

    /** The sling home url */
    private URL slingHomeUrl;

    /** The set of run modes . */
    private Set<String> runModes;

    private Configuration configuration;

    /** The name of the data file holding the sling id. */
    private static final String ID_FILE = "sling.id.file";

    /** The name of the data file holding install run mode options */
    private static final String OPTIONS_FILE = "sling.options.file";

    @ObjectClassDefinition(id = "org.apache.sling.settings.impl.SlingSettingsServiceImpl", name = "Apache Sling Settings Service", 
            description = "The settings service manages some basic settings of Sling like run modes or information about the current instance.")
    static @interface Configuration {
        @AttributeDefinition(name = "Instance Name", description = "A human readable name for the current instance.")
        String sling_name();

        @AttributeDefinition(name = "Instance Description", description = "A human readable description for the current instance.")
        String sling_description();
    }

    /** Create the service and search the Sling home urls and get/create a sling id. Setup run modes
     * 
     * @param context The bundle context */
    @Activate
    public SlingSettingsServiceImpl(final Configuration configuration, final BundleContext context) {
        this.configuration = configuration;
        this.setupSlingHome(context);
        this.setupSlingId(context);
        this.setupRunModes(context);
    }
   
    /**
     * Constructor only to be used from tests
     * @param runModes
     */
    public SlingSettingsServiceImpl(String runModes) {
        this.runModes = parseRunModes(runModes);
    }

    /** Get sling home and sling home URL */
    private void setupSlingHome(final BundleContext context) {
        this.slingHome = context.getProperty(SLING_HOME);
        final String url = context.getProperty(SLING_HOME_URL);
        if (url != null) {
            try {
                this.slingHomeUrl = new URL(url);
            } catch (MalformedURLException e) {
                logger.error("Sling home url is not a url: {}", url);
            }
        }
    }

    /** Get / create sling id */
    private void setupSlingId(final BundleContext context) {
        // try to read the id from the id file first
        final File idFile = context.getDataFile(ID_FILE);
        if (idFile == null) {
            // the osgi framework does not support storing something in the file system
            throw new RuntimeException("Unable to read from bundle data file.");
        }

        try {
            slingId = SlingIdUtil.readSlingId(idFile);
            logger.info("Read Sling ID {} from file {}", slingId, idFile);
        } catch (final Throwable t) {
            logger.error("Failed reading Sling ID from file " + idFile, t);
        }

        // no sling id yet or failure to read file: create an id and store
        if (slingId == null) {
            slingId = SlingIdUtil.createSlingId();
            logger.info("Created new Sling ID {}", slingId);
            try {
                SlingIdUtil.writeSlingId(idFile, slingId);
            } catch (final Throwable t) {
                logger.error("Failed writing Sling ID to file " + idFile, t);
            }
        }
    }

    static final class Options implements Serializable {
        private static final long serialVersionUID = 1L;
        String[] modes;
        String selected;
    }

    private List<Options> handleOptions(final Set<String> modesSet, final String propOptions) {
        final List<Options> optionsList = new ArrayList<Options>();
        if (propOptions != null && propOptions.trim().length() > 0) {

            final String[] options = propOptions.trim().split("\\|");
            for (final String opt : options) {
                String selected = null;
                final String[] modes = opt.trim().split(",");
                for (int i = 0; i < modes.length; i++) {
                    modes[i] = modes[i].trim();
                    if (selected != null) {
                        modesSet.remove(modes[i]);
                    } else {
                        if (modesSet.contains(modes[i])) {
                            selected = modes[i];
                        }
                    }
                }
                if (selected == null) {
                    selected = modes[0];
                    modesSet.add(modes[0]);
                }
                final Options o = new Options();
                o.selected = selected;
                o.modes = modes;
                optionsList.add(o);
            }
        }
        return optionsList;
    }

    private Set<String> parseRunModes(String runModes) {
        final Set<String> modesSet = new HashSet<>();
        final String[] modes = runModes.split(",");
        for (int i = 0; i < modes.length; i++) {
            modesSet.add(modes[i].trim());
        }
        return modesSet;
    }

    /** Set up run modes. */
    private void setupRunModes(final BundleContext context) {
        final Set<String> modesSet;

        // check configuration property first
        final String prop = context.getProperty(RUN_MODES_PROPERTY);
        if (prop != null && prop.trim().length() > 0) {
            modesSet = parseRunModes(prop);
        } else {
            modesSet = new HashSet<>();
        }

        // handle configured options
        this.handleOptions(modesSet, context.getProperty(RUN_MODE_OPTIONS));

        // handle configured install options
        // read persisted options if restart or update
        final List<Options> storedOptions = readOptions(context);
        if (storedOptions != null) {
            for (final Options o : storedOptions) {
                for (final String m : o.modes) {
                    modesSet.remove(m);
                }
                modesSet.add(o.selected);
            }
        }

        // now install options
        final List<Options> optionsList = this.handleOptions(modesSet, context.getProperty(RUN_MODE_INSTALL_OPTIONS));
        // and always save new install options
        writeOptions(context, optionsList);

        // make the set unmodifiable, as always the same set will be returned
        this.runModes = Collections.unmodifiableSet(modesSet);
        if (this.runModes.size() > 0) {
            logger.info("Active run modes: {}", this.runModes);
        } else {
            logger.info("No run modes active");
        }
    }

    @SuppressWarnings("unchecked")
    private List<Options> readOptions(final BundleContext context) {
        List<Options> optionsList = null;
        final File file = context.getDataFile(OPTIONS_FILE);
        if (file.exists()) {
            FileInputStream fis = null;
            ObjectInputStream ois = null;
            try {
                fis = new FileInputStream(file);
                ois = new ObjectInputStream(fis);

                optionsList = (List<Options>) ois.readObject();
            } catch (final IOException ioe) {
                throw new RuntimeException("Unable to read from options data file.", ioe);
            } catch (ClassNotFoundException cnfe) {
                throw new RuntimeException("Unable to read from options data file.", cnfe);
            } finally {
                if (ois != null) {
                    try {
                        ois.close();
                    } catch (final IOException ignore) {
                    }
                }
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (final IOException ignore) {
                    }
                }
            }
        }
        return optionsList;
    }

    void writeOptions(final BundleContext context, final List<Options> optionsList) {
        final File file = context.getDataFile(OPTIONS_FILE);
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        try {
            fos = new FileOutputStream(file);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(optionsList);
        } catch (final IOException ioe) {
            throw new RuntimeException("Unable to write to options data file.", ioe);
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (final IOException ignore) {
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (final IOException ignore) {
                }
            }
        }
    }

    /** @see org.apache.sling.settings.SlingSettingsService#getAbsolutePathWithinSlingHome(String) */
    @Override
    public String getAbsolutePathWithinSlingHome(final String relativePath) {
        return new File(slingHome, relativePath).getAbsolutePath();
    }

    /** @see org.apache.sling.settings.SlingSettingsService#getSlingId() */
    @Override
    public String getSlingId() {
        return this.slingId;
    }

    /** @see org.apache.sling.settings.SlingSettingsService#getSlingHome() */
    @Override
    public URL getSlingHome() {
        return this.slingHomeUrl;
    }

    /** @see org.apache.sling.settings.SlingSettingsService#getSlingHomePath() */
    @Override
    public String getSlingHomePath() {
        return this.slingHome;
    }

    /** @see org.apache.sling.settings.SlingSettingsService#getRunModes() */
    @Override
    public Set<String> getRunModes() {
        return this.runModes;
    }

    @Override
    public int getBestRunModeMatchCountFromSpec(String spec) {
        return getBestRunModeMatchCountFromSpec(spec, runModes);
    }

    static int getBestRunModeMatchCountFromSpec(String spec, Collection<String> activeRunModes) {
        int numMatchingRunModes = 0;
        // 1. support OR
        for (String discjunctivePart : spec.split(Pattern.quote(RUN_MODE_SPEC_OR_SEPARATOR))) {
            int newNumMatchingRunModes = getBestRunModeMatchCountFromConjunctions(discjunctivePart, activeRunModes);
            if (newNumMatchingRunModes > numMatchingRunModes) {
                numMatchingRunModes = newNumMatchingRunModes;
            }
        }
        return numMatchingRunModes;
    }

    static int getBestRunModeMatchCountFromConjunctions(String conjunctions, Collection<String> activeRunModes) {
        int numMatchingRunModes = 0;
        // 2. support AND
        for (String conjunctivePart : conjunctions.split(Pattern.quote(RUN_MODE_SPEC_AND_SEPARATOR))) {
            // 3. support NOT operator
            if (conjunctivePart.startsWith(RUN_MODE_SPEC_NOT_PREFIX)) {
                if (activeRunModes.contains(conjunctivePart.substring(RUN_MODE_SPEC_NOT_PREFIX.length()))) {
                    return 0;
                }
            } else {
                if (!activeRunModes.contains(conjunctivePart)) {
                    return 0;
                }
            }
            numMatchingRunModes++;
        }
        return numMatchingRunModes;
    }

    /**
     * @see org.apache.sling.settings.SlingSettingsService#getSlingName()
     */
    @Override
    public String getSlingName() {
        String name = configuration.sling_name();
        if ( name == null ) {
            name = "Instance " + this.slingId; // default
        }
        return name;
    }

    /** @see org.apache.sling.settings.SlingSettingsService#getSlingDescription() */
    @Override
    public String getSlingDescription() {
        String desc = configuration.sling_description();
        if (desc == null) {
            desc = "Instance with id " + this.slingId + " and run modes " + this.getRunModes(); // default
        }
        return desc;
    }

    /** Update the configuration of this service */
    @Modified
    public void update(final Configuration configuration) {
        // TODO is configuration thread safe i.e. new object per call?
        this.configuration = configuration;
    }
}
