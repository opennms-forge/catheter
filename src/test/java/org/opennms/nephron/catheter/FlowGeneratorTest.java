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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

public class FlowGeneratorTest {
    private final static long BPS = 1_000_000;
    private final static Instant PIT = Instant.ofEpochMilli(1500_000_000_000L);
    private final static long TICK_MS = 250;

    @Test
    public void moreFlowTest() {
        generatorTestLoop(2);
        generatorTestLoop(3);
        generatorTestLoop(4);
        generatorTestLoop(5);
        generatorTestLoop(6);
        generatorTestLoop(7);
        generatorTestLoop(8);
        generatorTestLoop(9);
        generatorTestLoop(10);
        generatorTestLoop(11);
        generatorTestLoop(12);
        generatorTestLoop(13);
        generatorTestLoop(14);
        generatorTestLoop(15);
        generatorTestLoop(16);
        generatorTestLoop(17);
        generatorTestLoop(18);
        generatorTestLoop(19);
        generatorTestLoop(20);
    }

    @Test
    public void oneFlowTest() {
        generatorTestLoop(1);
    }

    public void generatorTestLoop(final int maxFlowCount) {
        final Random random = new Random(12345L);
        final FlowGenerator flowGenerator = FlowGenerator.builder()
                .withMaxFlowCount(maxFlowCount)
                .withMinFlowDuration(Duration.ofSeconds(2))
                .withMaxFlowDuration(Duration.ofSeconds(10))
                .withActiveTimeout(Duration.ofSeconds(1))
                .withBytesPerSecond(BPS)
                .build(PIT, random);

        final List<FlowReport> flowReportList = new ArrayList<>();

        int i;

        for (i = 1; i < 1000; i++) {
            flowReportList.addAll(flowGenerator.tick(PIT.plus(Duration.ofMillis(i * TICK_MS))));
        }
        flowReportList.addAll(flowGenerator.shutdown(PIT.plus(Duration.ofMillis(i * TICK_MS))));

        double rate = flowReportList.stream().mapToDouble(FlowReport::getBytes).sum() / ((double) ((i - 1) * TICK_MS) / 1000.0);
        assertThat((long) rate, is(BPS));
    }

    @Test
    public void generatorTest() {
        final Random random = new Random(12345L);
        final FlowGenerator flowGenerator = FlowGenerator.builder()
                .withMaxFlowCount(10)
                .withMinFlowDuration(Duration.ofSeconds(1))
                .withMaxFlowDuration(Duration.ofSeconds(20))
                .withActiveTimeout(Duration.ofSeconds(2))
                .withBytesPerSecond(BPS)
                .build(PIT, random);

        assertThat(flowGenerator.tick(PIT.plus(Duration.ofMillis(500))), is(empty()));

        assertThat(flowGenerator.tick(PIT.plus(Duration.ofMillis(1000))), is(empty()));

        assertThat(flowGenerator.tick(PIT.plus(Duration.ofMillis(1500))), is(empty()));

        assertThat(flowGenerator.tick(PIT.plus(Duration.ofMillis(2000))), is(empty()));

        assertThat(flowGenerator.tick(PIT.plus(Duration.ofMillis(2500))), containsInAnyOrder(
                new FlowReport(PIT.plus(Duration.ofMillis(500)), PIT.plus(Duration.ofMillis(2500)), 1000000L),
                new FlowReport(PIT.plus(Duration.ofMillis(500)), PIT.plus(Duration.ofMillis(2500)), 1000000L)
        ));

        assertThat(flowGenerator.tick(PIT.plus(Duration.ofMillis(3000))), is(empty()));

        assertThat(flowGenerator.tick(PIT.plus(Duration.ofMillis(3500))), is(empty()));

        assertThat(flowGenerator.tick(PIT.plus(Duration.ofMillis(4000))), is(empty()));

        assertThat(flowGenerator.tick(PIT.plus(Duration.ofMillis(4500))), containsInAnyOrder(
                new FlowReport(PIT.plus(Duration.ofMillis(2500)), PIT.plus(Duration.ofMillis(4500)), 1000000L),
                new FlowReport(PIT.plus(Duration.ofMillis(2500)), PIT.plus(Duration.ofMillis(4500)), 1000000L)
        ));

        assertThat(flowGenerator.tick(PIT.plus(Duration.ofMillis(5000))), is(empty()));

        assertThat(flowGenerator.tick(PIT.plus(Duration.ofMillis(5500))), is(empty()));

        assertThat(flowGenerator.tick(PIT.plus(Duration.ofMillis(6000))), is(empty()));

        assertThat(flowGenerator.tick(PIT.plus(Duration.ofMillis(6500))), containsInAnyOrder(
                new FlowReport(PIT.plus(Duration.ofMillis(4500)), PIT.plus(Duration.ofMillis(6500)), 1000000L),
                new FlowReport(PIT.plus(Duration.ofMillis(4500)), PIT.plus(Duration.ofMillis(6500)), 1000000L)
        ));

        assertThat(flowGenerator.tick(PIT.plus(Duration.ofMillis(7000))), is(empty()));

        assertThat(flowGenerator.tick(PIT.plus(Duration.ofMillis(7500))), containsInAnyOrder(
                new FlowReport(PIT.plus(Duration.ofMillis(6500)), PIT.plus(Duration.ofMillis(7500)), 500000L)
        ));

        assertThat(flowGenerator.tick(PIT.plus(Duration.ofMillis(8000))), is(empty()));

        assertThat(flowGenerator.tick(PIT.plus(Duration.ofMillis(8500))), containsInAnyOrder(
                new FlowReport(PIT.plus(Duration.ofMillis(6500)), PIT.plus(Duration.ofMillis(8500)), 1000000L)
        ));

        assertThat(flowGenerator.tick(PIT.plus(Duration.ofMillis(9000))), is(empty()));

        assertThat(flowGenerator.tick(PIT.plus(Duration.ofMillis(9500))), containsInAnyOrder(
                new FlowReport(PIT.plus(Duration.ofMillis(7500)), PIT.plus(Duration.ofMillis(9500)), 333332L),
                new FlowReport(PIT.plus(Duration.ofMillis(7500)), PIT.plus(Duration.ofMillis(9500)), 333332L),
                new FlowReport(PIT.plus(Duration.ofMillis(7500)), PIT.plus(Duration.ofMillis(9500)), 333336L)
        ));
    }
}
