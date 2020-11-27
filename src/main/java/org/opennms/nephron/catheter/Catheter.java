/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2020 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2020 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.nephron.catheter;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;


public class Catheter {

    @Argument
    private File jsonConfigFile;

    public static void main(final String... args) throws IOException, JAXBException {
        new Catheter().run(args);
    }

    private void run(final String... args) throws IOException, JAXBException {
        final CmdLineParser parser = new CmdLineParser(this);

        try {
            parser.parseArgument(args);

            if (jsonConfigFile == null) {
                throw new CmdLineException(parser, "No argument is given");
            }
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            System.err.println("java -jar catheter-1.0-SNAPSHOT-jar-with-dependencies.jar JSON-file");
            parser.printUsage(System.err);
            System.err.println();

            return;
        }

        final Simulation simulation = Simulation.fromFile(jsonConfigFile);

        simulation.start();
    }
}
