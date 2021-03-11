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

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CatheterIT {
    private static final Logger LOG = LoggerFactory.getLogger(CatheterIT.class);

    public static class TrackingHandler implements BiConsumer<Exporter, FlowReport> {

        private final AtomicLong count = new AtomicLong();
        private final AtomicLong bytes = new AtomicLong();
        private final List<FlowReport> flows = new ArrayList<>();

        @Override
        public void accept(final Exporter exporter, final FlowReport report) {
            this.count.incrementAndGet();
            this.bytes.addAndGet(report.getBytes());
            this.flows.add(report);
        }

        public long getReceivedCount() {
            return this.count.get();
        }

        public long getReceivedBytes() {
            return this.bytes.get();
        }

        public List<FlowReport> getReceivedFlows() {
            return Collections.unmodifiableList(this.flows);
        }

        public void awaitRecords(final long count) {
            await().pollDelay(Duration.ofSeconds(1))
                   .atMost(Duration.ofMinutes(1))
                   .until(() -> this.getReceivedCount() >= count);
        }
    }

    @Test
    public void testTimestampsNonRealtime() throws Exception {
        final Instant now = Instant.ofEpochMilli(1_500_000_000_000L);

        final TrackingHandler handler = new TrackingHandler();

        final Simulation simulation = Simulation.builder(handler)
                .withRealtime(false)
                .withStartTime(now)
                .withTickMs(Duration.ofMillis(50))
                .withExporters(
                        Exporter.builder()
                                .withNodeId(1)
                                .withForeignSource("exporters")
                                .withForeignId("test1")
                                .withInputSnmp(98)
                                .withOutputSnmp(99)
                                .withGenerator(FlowGenerator.builder()
                                        .withBytesPerSecond(750_000L)
                                        .withMaxFlowCount(10)
                                        .withActiveTimeout(Duration.ofSeconds(2))
                                        .withMinFlowDuration(Duration.ofSeconds(1))
                                        .withMaxFlowDuration(Duration.ofSeconds(20)))
                ).build();

        // run first simulation with seed
        simulation.start(20);
        simulation.join();

        // wait till all data arrived
        handler.awaitRecords(simulation.getFlowsSent());

        final Instant future = now.plus(simulation.getElapsedTime()).plus(Duration.ofMillis(1));

        LOG.debug("Now: " + now + " Future: " + future);

        for (final FlowReport flow : handler.getReceivedFlows()) {
            final Instant start = flow.getStart();
            final Instant end = flow.getEnd();

            LOG.debug("Start: " + start + " End: " + end);

            assertTrue(start.isBefore(end));
            assertTrue(start.isAfter(now));
            assertTrue(end.isAfter(now));
            assertTrue(end.isBefore(future));
        }
    }

    @Test
    public void testTimestampsRealtime() throws Exception {
        final Instant now = Instant.now();

        final TrackingHandler handler = new TrackingHandler();

        final Simulation simulation = Simulation.builder(handler)
                .withRealtime(true)
                .withStartTime(now)
                .withTickMs(Duration.ofMillis(50))
                .withExporters(
                        Exporter.builder()
                                .withNodeId(1)
                                .withForeignSource("exporters")
                                .withForeignId("test1")
                                .withInputSnmp(98)
                                .withOutputSnmp(99)
                                .withGenerator(FlowGenerator.builder()
                                        .withBytesPerSecond(750_000L)
                                        .withMaxFlowCount(10)
                                        .withActiveTimeout(Duration.ofSeconds(2))
                                        .withMinFlowDuration(Duration.ofSeconds(1))
                                        .withMaxFlowDuration(Duration.ofSeconds(20)))
                ).build();

        // run first simulation with seed
        simulation.start();

        Thread.sleep(5000);

        simulation.stop();
        simulation.join();

        // wait till all data arrived
        handler.awaitRecords(simulation.getFlowsSent());

        final Instant future = Instant.now();

        LOG.debug("Now: " + now + " Future: " + future);

        for (final FlowReport flow : handler.getReceivedFlows()) {
            final Instant start = flow.getStart();
            final Instant end = flow.getEnd();

            LOG.debug("Start: " + start + " End: " + end);

            assertTrue(!start.isAfter(end));
            assertTrue(start.isAfter(now));
            assertTrue(end.isAfter(now));
            assertTrue(end.isBefore(future));
        }
    }

    @Test
    public void testInputAndOutput() throws Exception {
        final Instant now = Instant.ofEpochMilli(1_500_000_000_000L);

        final TrackingHandler handler = new TrackingHandler();

        final BiConsumer<Exporter, FlowReport> checkingHandler = handler.andThen((exporter, report) -> {
            assertThat(exporter.getInputSnmp(), is(98));
            assertThat(exporter.getOutputSnmp(), is(99));
        });

        final Simulation simulation = Simulation.builder(checkingHandler)
                .withRealtime(false)
                .withStartTime(now)
                .withTickMs(Duration.ofMillis(50))
                .withExporters(
                        Exporter.builder()
                                .withNodeId(1)
                                .withForeignSource("exporters")
                                .withForeignId("test1")
                                .withInputSnmp(98)
                                .withOutputSnmp(99)
                                .withClockOffset(Duration.ofSeconds(-10))
                                .withGenerator(FlowGenerator.builder()
                                        .withBytesPerSecond(750_000L)
                                        .withMaxFlowCount(10)
                                        .withActiveTimeout(Duration.ofSeconds(2))
                                        .withMinFlowDuration(Duration.ofSeconds(1))
                                        .withMaxFlowDuration(Duration.ofSeconds(20)))
                ).build();

        // run simulation
        simulation.start(20);
        simulation.join();

        // wait till all data arrived
        handler.awaitRecords(simulation.getFlowsSent());
    }

    @Test
    public void testSeed() throws Exception {
        // create random seed
        long seed = new Random().nextLong();

        // run simulation with same seed twice
        final TrackingHandler handler1 = runSimulation( false, false, seed, null, 100_000L);
        final TrackingHandler handler2 = runSimulation( false, false, seed, null, 100_000L);

        // check whether the results ot the two simulation runs are the same
        assertEquals(handler1.getReceivedBytes(), handler2.getReceivedBytes());
        assertEquals(handler1.getReceivedCount(), handler2.getReceivedCount());
        assertEquals(handler1.getReceivedFlows(), handler2.getReceivedFlows());
    }

    @Test
    public void testRealtime() {
        runSimulation(true, false, null, Duration.ofSeconds(5), null);
    }

    @Test
    public void testRealtimeWithClockSkew() {
        runSimulation(true, true, 123456L, Duration.ofSeconds(5), null);
    }

    @Test
    public void testNonRealtime() {
        runSimulation(false, false, null, Duration.ofSeconds(5), null);
    }

    @Test
    public void testNonRealtimeWithClockSkew() {
        runSimulation(false, true, 123456L, Duration.ofSeconds(5), null);
    }

    public TrackingHandler runSimulation(final boolean realtime, final boolean clockSkew, final Long seed, final Duration duration, final Long iterations) {
        final TrackingHandler handler = new TrackingHandler();

        final Simulation simulation = Simulation.builder(handler)
                                                .withRealtime(realtime)
                                                .withStartTime(realtime ? Instant.now() : Instant.ofEpochMilli(1_500_000_000_000L))
                                                .withTickMs(Duration.ofMillis(250))
                                                .withExporters(
                                                        Exporter.builder()
                                                                .withNodeId(1)
                                                                .withForeignSource("exporters")
                                                                .withForeignId("test1")
                                                                .withClockOffset(clockSkew ? Duration.ofSeconds(-10) : Duration.ZERO)
                                                                .withGenerator(FlowGenerator.builder()
                                                                                            .withBytesPerSecond(750_000L)
                                                                                            .withMaxFlowCount(10)
                                                                                            .withActiveTimeout(Duration.ofSeconds(2))
                                                                                            .withMinFlowDuration(Duration.ofSeconds(1))
                                                                                            .withMaxFlowDuration(Duration.ofSeconds(20))),
                                                        Exporter.builder()
                                                                .withNodeId(2)
                                                                .withForeignSource("exporters")
                                                                .withForeignId("test2")
                                                                .withClockOffset(clockSkew ? Duration.ofSeconds(10) : Duration.ZERO)
                                                                .withGenerator(FlowGenerator.builder()
                                                                                            .withBytesPerSecond(250_000L)
                                                                                            .withMaxFlowCount(10)
                                                                                            .withActiveTimeout(Duration.ofSeconds(1))
                                                                                            .withMinFlowDuration(Duration.ofSeconds(2))
                                                                                            .withMaxFlowDuration(Duration.ofSeconds(15)))
                                                              )
                                                .withSeed(seed != null ? seed : System.currentTimeMillis())
                                                .build();

        if (iterations != null) {
            simulation.start(iterations);
        } else {
            simulation.start();
        }

        if (duration != null) {
            try {
                Thread.sleep(duration.toMillis());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            simulation.stop();
        }

        try {
            simulation.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        final long rateSent = (long) ((double) simulation.getBytesSent() / (double) simulation.getElapsedTime().toMillis() * 1000.0);

        LOG.debug("Simulation took {} ms", simulation.getElapsedTime().toMillis());
        LOG.debug("Simulation reported {} flows", simulation.getFlowsSent());
        LOG.debug("Simulation reported {} bytes in total", simulation.getBytesSent());
        LOG.debug("Simulation rate was {} byte/sec", rateSent);

        for (int i = -40; i < 40; i++) {
            final long rateSentX = (long) ((double) simulation.getBytesSent() / (double) ((i * 250) + simulation.getElapsedTime().toMillis()) * 1000.0);
            LOG.debug("Simulation rate (" + (i * 250) + "ms) was {} byte/sec", rateSentX);
        }

        assertThat(rateSent, is(1000000L));

        handler.awaitRecords(simulation.getFlowsSent());

        final long rateReceived = (long) ((double) handler.getReceivedBytes() / (double) simulation.getElapsedTime().toMillis() * 1000.0);

        LOG.debug("Received {} flows", handler.getReceivedCount());
        LOG.debug("Received {} bytes in total", handler.getReceivedBytes());
        LOG.debug("Rate is {} byte/sec", rateReceived);

        assertThat(handler.getReceivedBytes(), is(simulation.getBytesSent()));
        assertThat(rateReceived, is(1000000L));

        return handler;
    }
}
