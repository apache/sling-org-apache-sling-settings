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

import java.io.PrintStream;
import java.util.Set;

import org.apache.felix.shell.Command;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.component.propertytypes.ServiceVendor;

/**
 * Run mode command for the shell.
 */
@Component
@ServiceVendor("The Apache Software Foundation")
@ServiceDescription("Apache Sling Sling Run Mode Shell Command")
public class RunModeCommand implements Command {

    private static final String CMD_NAME = "runmodes";

    private final Set<String> modes;

    @Activate
    public RunModeCommand(final BundleContext btx, @Reference SlingSettingsService slingSettings) {
        this.modes = slingSettings.getRunModes();
    }

    /**
     * @see org.apache.felix.shell.Command#getName()
     */
    @Override
    public String getName() {
        return CMD_NAME;
    }

    /**
     * @see org.apache.felix.shell.Command#getShortDescription()
     */
    @Override
    public String getShortDescription() {
        return "lists current run modes";
    }

    /**
     * @see org.apache.felix.shell.Command#getUsage()
     */
    @Override
    public String getUsage() {
        return CMD_NAME;
    }

    /**
     * @see org.apache.felix.shell.Command#execute(java.lang.String, java.io.PrintStream, java.io.PrintStream)
     */
    @Override
    public void execute(String command, PrintStream out, PrintStream err) {
        out.print("Current Run Modes: ");
        if (modes == null || modes.isEmpty()) {
            out.println("-");
        } else {
            out.println(modes);
        }
    }
}
