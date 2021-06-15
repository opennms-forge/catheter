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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Simulation {
    private static final Logger LOG = LoggerFactory.getLogger(Simulation.class);

    private final BiConsumer<Exporter, FlowReport> handler;
    private final Duration tickMs;
    private final boolean realtime;
    private final Instant startTime;
    private final List<Exporter> exporters;
    private Thread thread;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Duration elapsedTime = Duration.ZERO;
    private long flowsSent = 0;
    private long bytesSent = 0;
    private final Random random = new Random();
    private long maxIterations = 0;

    private Simulation(final Builder builder) {
        this.handler = builder.handler;
        this.tickMs = Objects.requireNonNull(builder.tickMs);
        this.realtime = builder.realtime;
        this.startTime = Instant.ofEpochMilli(builder.startTime != null ? builder.startTime.toEpochMilli() : Instant.now().toEpochMilli() / builder.tickMs.toMillis() * builder.tickMs.toMillis());
        this.random.setSeed(builder.seed);
        this.exporters = builder.exporters.stream().map(e -> e.build(this.startTime, random)).collect(Collectors.toList());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Simulation that = (Simulation) o;
        return this.realtime == that.realtime &&
                Objects.equals(this.tickMs, that.tickMs) &&
                Objects.equals(this.startTime, that.startTime) &&
                Objects.equals(this.exporters, that.exporters);
    }

    @Override
    public String toString() {
        return "Simulation{" +
                ", tickMs=" + this.tickMs +
                ", realtime=" + this.realtime +
                ", startTime=" + this.startTime +
                ", exporters=" + this.exporters +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.tickMs, this.realtime, this.startTime, this.exporters, this.thread, this.running, this.elapsedTime, this.flowsSent, this.bytesSent, this.random, this.maxIterations);
    }

    public static Builder builder(final BiConsumer<Exporter, FlowReport> handler) {
        return new Builder(handler);
    }

    public void start() {
        start(0);
    }

    public void start(final long maxIterations) {
        this.maxIterations = maxIterations;
        if (!this.running.get()) {
            this.running.set(true);
            this.thread = new Thread(this::run);
            this.thread.start();
        }
    }

    private void run() {
        this.elapsedTime = Duration.ZERO;
        this.flowsSent = 0;
        this.bytesSent = 0;

        Instant now = this.startTime;

        while (this.running.get()) {
            now = now.plus(this.tickMs);
            this.elapsedTime = Duration.between(this.startTime, now);

            if (this.maxIterations > 0) {
                this.maxIterations--;
                if (this.maxIterations == 0) {
                    this.running.set(false);
                }
            }

            if (this.realtime) {
                final Duration timeToSleep = Duration.between(Instant.now(), now);
                if (!timeToSleep.isNegative()) {
                    try {
                        LOG.trace("Sleeping for {} ...", timeToSleep);
                        Thread.sleep(timeToSleep.toMillis());
                    } catch (InterruptedException e) {
                        LOG.warn("Simulation: exception while Thread.sleep()", e);
                    }
                }
            }


            for (final Exporter exporter : this.exporters) {
                dispatch(exporter, exporter.tick(now));
            }
        }

        LOG.debug("Simulation: shutting down {} exporters", this.exporters.size());

        for (final Exporter exporter : this.exporters) {
            dispatch(exporter, exporter.shutdown(now));
        }
    }

    private void dispatch(final Exporter exporter, final Collection<FlowReport> flowReports) {
        this.flowsSent += flowReports.size();
        for (final FlowReport flowReport : flowReports) {
            this.bytesSent += flowReport.getBytes();

            this.handler.accept(exporter, flowReport);
        }
    }

    public void join() throws InterruptedException {
        if (this.thread != null) {
            this.thread.join();
        }
    }

    public void stop() {
        if (this.running.get()) {
            this.running.set(false);
        }
    }

    public Duration getElapsedTime() {
        return this.elapsedTime;
    }

    public long getFlowsSent() {
        return this.flowsSent;
    }

    public long getBytesSent() {
        return this.bytesSent;
    }

    public Random getRandom() {
        return this.random;
    }

    public BiConsumer<Exporter, FlowReport> getHandler() {
        return this.handler;
    }

    public static class Builder {
        public long seed = new Random().nextLong();

        private BiConsumer<Exporter, FlowReport> handler;

        private Duration tickMs = Duration.ofMillis(250);
        private boolean realtime;
        private Instant startTime;
        private final List<Exporter.Builder> exporters = new ArrayList<>();

        private Builder(final BiConsumer<Exporter, FlowReport> handler) {
            this.handler = Objects.requireNonNull(handler);
        }

        public Builder withHandler(final BiConsumer<Exporter, FlowReport> handler) {
            this.handler = Objects.requireNonNull(handler);
            return this;
        }

        public Builder withTickMs(final Duration tickMs) {
            this.tickMs = Objects.requireNonNull(tickMs);
            return this;
        }

        public Builder withRealtime(final boolean realtime) {
            this.realtime = realtime;
            return this;
        }

        public Builder withStartTime(final Instant startTime) {
            this.startTime = Objects.requireNonNull(startTime);
            return this;
        }

        public Simulation build() {
            return new Simulation(this);
        }

        public Builder withExporters(final Exporter.Builder... builders) {
            this.exporters.addAll(Arrays.asList(builders));
            return this;
        }

        public Builder withExporters(final Collection<Exporter.Builder> builders) {
            this.exporters.addAll(builders);
            return this;
        }

        public Builder withSeed(final long seed) {
            this.seed = seed;
            return this;
        }
    }
}
