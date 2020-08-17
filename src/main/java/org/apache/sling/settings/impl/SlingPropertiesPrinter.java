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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a configuration printer for the web console which
 * prints out the Sling properties from Launchpad if available.
 */
public class SlingPropertiesPrinter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SlingPropertiesPrinter.class);
    
    // Called from bundle activator
    public static void init(BundleContext bundleContext) throws IOException {
        // if the properties are available, we register the sling properties plugin
        Properties props;
        final String propUrl = bundleContext.getProperty("sling.properties.url");
        if ( propUrl != null ) {
            // try to read properties
            try (final InputStream is = new URL(propUrl).openStream()) {
                final Properties tmp = new Properties();
                tmp.load(is);
                // update props
                for(final Object key : tmp.keySet()) {
                    final Object value = bundleContext.getProperty(key.toString());
                    if ( value != null ) {
                        tmp.put(key, value);
                    }
                }
                props = tmp;

            } catch (IOException ioe) {
                LOGGER.error("Unable to read sling properties from '{}'", propUrl, ioe);
                return;
            }
        } else {
           LOGGER.debug("No bundle context property 'sling.properties.url' provided, not starting 'slingprops' webconsole plugin!");
           return;
        }
        final SlingPropertiesPrinter propertiesPrinter = new SlingPropertiesPrinter(props);
        final Dictionary<String, String> serviceProps = new Hashtable<>();
        serviceProps.put("felix.webconsole.label", "slingprops");
        serviceProps.put("felix.webconsole.title", "Sling Properties");
        serviceProps.put("felix.webconsole.configprinter.modes", "always");

        // no need to keep serviceregistration return value as deregistration only happens automatically once bundle stops
        bundleContext.registerService(SlingPropertiesPrinter.class.getName(),
                propertiesPrinter,
                serviceProps);
    }


    private static String HEADLINE = "Apache Sling Launchpad Properties";

    private final Properties props;

    public SlingPropertiesPrinter(Properties props) throws IOException {
        this.props = props;
    }


    /**
     * Print out the servlet filter chains.
     * @see org.apache.felix.webconsole.ConfigurationPrinter#printConfiguration(java.io.PrintWriter)
     */
    public void printConfiguration(PrintWriter pw) {
        pw.println(HEADLINE);
        pw.println();
        SortedSet<Object> keys = new TreeSet<Object>( props.keySet() );
        for ( Iterator<Object> ki = keys.iterator(); ki.hasNext(); ) {
            final Object key = ki.next();
            pw.print( key );
            pw.print(" = ");
            final Object value = props.get(key);
            if ( value != null ) {
                pw.print(value.toString());
            }
            pw.println();
        }
    }

    /**
     * @see org.apache.felix.webconsole.ModeAwareConfigurationPrinter#printConfiguration(java.io.PrintWriter, java.lang.String)
     */
    public void printConfiguration(PrintWriter printWriter, String mode) {
        if ( ! "zip".equals(mode) ) {
            this.printConfiguration(printWriter);
        } else {
            // write into byte array first
            String contents = null;
            try {
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                this.props.store(baos , HEADLINE);
                contents = baos.toString("8859_1");
            } catch (IOException ioe) {
                // if something goes wrong here we default to text output
                this.printConfiguration(printWriter);
                return;
            }
            printWriter.write(contents);
        }
    }
}
