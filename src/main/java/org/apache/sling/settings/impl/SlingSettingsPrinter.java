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

import java.io.PrintWriter;

import org.apache.sling.settings.SlingSettingsService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This is a configuration printer for the web console which
 * prints out the sling settings.
 *
 */
@Component(service = SlingSettingsPrinter.class, property= {"felix.webconsole.label=slingsettings","felix.webconsole.title=Sling Settings","felix.webconsole.configprinter.modes=always"})
public class SlingSettingsPrinter {

    private static String HEADLINE = "Apache Sling Settings";

    private final SlingSettingsService settings;

    @Activate
    public SlingSettingsPrinter(@Reference final SlingSettingsService settings) {
        this.settings = settings;
    }

    /**
     * Print out the servlet filter chains.
     * @see org.apache.felix.webconsole.ConfigurationPrinter#printConfiguration(java.io.PrintWriter)
     */
    public void printConfiguration(PrintWriter pw) {
        pw.println(HEADLINE);
        pw.println();
        pw.print("Sling ID = ");
        pw.print(this.settings.getSlingId());
        pw.println();
        pw.print("Sling Name = ");
        pw.print(this.settings.getSlingName());
        pw.println();
        pw.print("Sling Description = ");
        pw.print(this.settings.getSlingDescription());
        pw.println();
        pw.print("Sling Home = ");
        pw.print(this.settings.getSlingHomePath());
        pw.println();
        pw.print("Sling Home URL = ");
        pw.print(this.settings.getSlingHome());
        pw.println();
        pw.print("Run Modes = ");
        pw.print(this.settings.getRunModes());
        pw.println();
    }
}
