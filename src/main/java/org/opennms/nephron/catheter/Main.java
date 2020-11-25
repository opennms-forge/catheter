package org.opennms.nephron.catheter;

import java.time.Duration;
import java.time.Instant;

import org.opennms.netmgt.flows.persistence.model.FlowDocument;

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

public class Main {
    private final static Duration TICK = Duration.ofMillis(100);

    public static void main(final String... args) {
        Instant now = Instant.ofEpochMilli(1_500_000_000_000L);

        final Exporter exporter = Exporter.builder()
                                          .withNodeId(1)
                                          .withForeignSource("exporters")
                                          .withForeignId("test")
                                          .withGenerator(FlowGenerator.builder()
                                                                      .withBytesPerSecond(1_000_000L)
                                                                      .withMaxFlowCount(10)
                                                                      .withActiveTimeout(Duration.ofSeconds(2))
                                                                      .withMinFlowDuration(Duration.ofSeconds(1))
                                                                      .withMaxFlowDuration(Duration.ofSeconds(20)))
                                          .build(now);

        long bytes = 0;

        for (int i = 0; i < 10_000; i++) {
            now = now.plus(TICK);

            for (final FlowDocument report : exporter.tick(now)) {
                bytes += report.getNumBytes().getValue();
            }
        }

        for (final FlowDocument report : exporter.shutdown(now)) {
            bytes += report.getNumBytes().getValue();
        }

        // Got 10_000 ticks with 0.1s and 1_000_000b/s = 1_000_000_000b
        System.out.println(bytes);
    }
}
