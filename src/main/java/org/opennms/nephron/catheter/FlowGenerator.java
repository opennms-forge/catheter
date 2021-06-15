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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import org.opennms.nephron.catheter.random.DurationZufall;
import org.opennms.nephron.catheter.random.IntegerZufall;
import org.opennms.nephron.catheter.random.Zufall;

import com.google.common.collect.Lists;

public class FlowGenerator {
    private final long bytesPerSecond;
    private final Zufall<Duration> flowDuration;
    private final int maxFlowCount;
    private final Duration activeTimeout;
    private final List<Flow> ongoingFlows = Lists.newArrayList();
    private Instant lastTick;
    private Random random;

    private FlowGenerator(final Builder builder, final Instant now, final Random random) {
        this.bytesPerSecond = builder.bytesPerSecond;

        this.flowDuration = new DurationZufall(random, builder.minFlowDuration, builder.maxFlowDuration);

        this.maxFlowCount = builder.maxFlowCount;
        this.activeTimeout = builder.activeTimeout;

        this.lastTick = now;
        this.random = random;
        // span flows from the very beginning
        // -> ensures that the required traffic volume is met from the very beginning
        spawnFlows(now);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Called for every tick instant.
     *
     * The first tick instant is start + tickMs.
     */
    public Collection<FlowReport> tick(final Instant now) {
        final Duration tick = Duration.ofMillis(now.toEpochMilli() - lastTick.toEpochMilli());
        final List<FlowReport> reports = Lists.newArrayList();

        // all ongoing flows get their share of the total number of bytes to transmit
        double tickDurationInSeconds = ((double) tick.toMillis()) / 1000.0;
        double bytesToTransmit = bytesPerSecond * tickDurationInSeconds;

        for (int i = ongoingFlows.size() - 1; i >= 0; i--) {
            Flow flow = ongoingFlows.get(i);
            long transmit = Math.round(i == 0 ? bytesToTransmit : tickDurationInSeconds * flow.getBytesPerSecond());
            flow.transmit(transmit);
            bytesToTransmit -= transmit;
        }

        // some flows report results because they
        // * reached their and or
        // * they hit the active timeout
        for (final Iterator<Flow> it = this.ongoingFlows.iterator(); it.hasNext(); ) {
            final Flow flow = it.next();

            // End flows, probability depends of the flow's duration
            final Duration duration = Duration.ofMillis(now.toEpochMilli() - flow.getStart().toEpochMilli());
            final Duration randomDuration = flowDuration.random();

            if (duration.toMillis() > randomDuration.toMillis()) {
                reports.add(flow.report(now));
                it.remove();
                continue;
            }

            // Check for flows with trigger active timeout
            if (flow.checkTimeout(now, this.activeTimeout)) {
                reports.add(flow.report(now));
            }
        }

        spawnFlows(now);

        this.lastTick = now;

        return reports;
    }

    private void spawnFlows(Instant now) {
        // compute the missing bytesPerSecond due to ended flows
        long deltaBytesPerSecond = this.bytesPerSecond - this.ongoingFlows.stream().mapToLong(Flow::getBytesPerSecond).sum();

        if (deltaBytesPerSecond > 0) {
            // determine the number of flows to spawn
            final IntegerZufall zl = new IntegerZufall(random, 1, maxFlowCount - ongoingFlows.size());
            int flowsToSpawn = zl.random();

            // if byte rate is to low reduce the number of flows
            while (flowsToSpawn > 1 && deltaBytesPerSecond / flowsToSpawn < 1000) {
                flowsToSpawn--;
            }

            // compute the share of byte rate for the flows to spawn
            final long share = deltaBytesPerSecond / flowsToSpawn;
            for (int i = 0; i < flowsToSpawn; i++) {
                // add the share or use the remaining byte rate to reduce the overall error
                final Flow flow = new Flow(now, i == flowsToSpawn - 1 ? deltaBytesPerSecond : share);
                deltaBytesPerSecond -= share;
                this.ongoingFlows.add(flow);
            }
        }
    }

    /**
     * Called for the last tick.
     *
     * The last tick happens if either the maximum number of simulation iterations did happen or if stop was called
     * on the simulation.
     */
    public Collection<FlowReport> shutdown(final Instant now) {
        // Generate reports for all ongoing flows
        final List<FlowReport> reports = Lists.newArrayList();
        for (final Flow flow : this.ongoingFlows) {
            reports.add(flow.report(now));
        }

        // Clear out the list of flows
        this.ongoingFlows.clear();

        return reports;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final FlowGenerator that = (FlowGenerator) o;
        return this.bytesPerSecond == that.bytesPerSecond &&
                this.maxFlowCount == that.maxFlowCount &&
                Objects.equals(this.flowDuration, that.flowDuration) &&
                Objects.equals(this.activeTimeout, that.activeTimeout) &&
                Objects.equals(this.lastTick, that.lastTick);
    }

    @Override
    public String toString() {
        return "FlowGenerator{" +
                "bytesPerSecond=" + this.bytesPerSecond +
                ", flowDuration=" + this.flowDuration +
                ", maxFlowCount=" + this.maxFlowCount +
                ", activeTimeout=" + this.activeTimeout +
                ", lastTick=" + this.lastTick +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.bytesPerSecond, this.flowDuration, this.maxFlowCount, this.activeTimeout, this.ongoingFlows, this.lastTick, this.random);
    }

    public static class Builder {
        private long bytesPerSecond = 1_000_000L;

        private Duration minFlowDuration = Duration.ofSeconds(1);
        private Duration maxFlowDuration = Duration.ofSeconds(30);

        private int maxFlowCount = 10;

        private Duration activeTimeout = Duration.ofSeconds(10);

        private Builder() {
        }

        public Builder withBytesPerSecond(final long bytesPerSecond) {
            this.bytesPerSecond = bytesPerSecond;
            return this;
        }

        public Builder withMinFlowDuration(final Duration minFlowDuration) {
            this.minFlowDuration = Objects.requireNonNull(minFlowDuration);
            return this;
        }

        public Builder withMaxFlowDuration(final Duration maxFlowDuration) {
            this.maxFlowDuration = Objects.requireNonNull(maxFlowDuration);
            return this;
        }

        public Builder withMaxFlowCount(final int maxFlowCount) {
            this.maxFlowCount = maxFlowCount;
            return this;
        }

        public Builder withActiveTimeout(final Duration activeTimeout) {
            this.activeTimeout = Objects.requireNonNull(activeTimeout);
            return this;
        }

        public FlowGenerator build(final Instant now, final Random random) {
            return new FlowGenerator(this, now, random);
        }
    }
}
