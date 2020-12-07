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
import java.util.stream.Collectors;

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
    }

    public static Builder builder() {
        return new Builder();
    }

    public Collection<FlowReport> tick(final Instant now) {
        final Duration tick = Duration.ofMillis(now.toEpochMilli() - lastTick.toEpochMilli());
        final List<FlowReport> reports = Lists.newArrayList();

        for (final Iterator<Flow> it = this.ongoingFlows.iterator(); it.hasNext(); ) {
            final Flow flow = it.next();

            // End flows, probability depends of the flow's duration
            final Duration duration = Duration.ofMillis(now.toEpochMilli() - flow.getStart().toEpochMilli());
            final Duration randomDuration = flowDuration.random();

            if (duration.toMillis() > randomDuration.toMillis()) {
                reports.add(flow.report(now));
                it.remove();
            }

            // Check for flows with trigger active timeout
            if (flow.checkTimeout(now, this.activeTimeout)) {
                reports.add(flow.report(now));
            }
        }

        // compute the missing bytesPerSecond due to ended flows
        long deltaBytesPerSecond = this.bytesPerSecond - this.ongoingFlows.stream().mapToLong(Flow::getBytesPerSecond).sum();

        if (deltaBytesPerSecond > 0) {
            final IntegerZufall zl = new IntegerZufall(random, 1, maxFlowCount - ongoingFlows.size());
            int flowsToSpawn = zl.random();

            while (flowsToSpawn > 1 && deltaBytesPerSecond / flowsToSpawn < 1000) {
                flowsToSpawn--;
            }

            final long share = deltaBytesPerSecond / flowsToSpawn;
            for (int i = 0; i < flowsToSpawn; i++) {
                final Flow flow = new Flow(now, i == flowsToSpawn - 1 ? deltaBytesPerSecond : share);
                deltaBytesPerSecond -= share;
                this.ongoingFlows.add(flow);
            }
        }

        double error = 0.0;

        double byteRate = ((double) tick.toMillis()) / 1000.0;

        for (final Flow flow : this.ongoingFlows) {
            double rate = Math.floor(error + byteRate * (double) flow.getBytesPerSecond());
            error = (error + byteRate * (double) flow.getBytesPerSecond()) - rate;
            flow.transmit((long) rate);
        }

        this.lastTick = now;

        return reports;
    }

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
        FlowGenerator that = (FlowGenerator) o;
        return bytesPerSecond == that.bytesPerSecond &&
                maxFlowCount == that.maxFlowCount &&
                Objects.equals(flowDuration, that.flowDuration) &&
                Objects.equals(activeTimeout, that.activeTimeout) &&
                Objects.equals(lastTick, that.lastTick);
    }

    @Override
    public String toString() {
        return "FlowGenerator{" +
                "bytesPerSecond=" + bytesPerSecond +
                ", flowDuration=" + flowDuration +
                ", maxFlowCount=" + maxFlowCount +
                ", activeTimeout=" + activeTimeout +
                ", lastTick=" + lastTick +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(bytesPerSecond, flowDuration, maxFlowCount, activeTimeout, ongoingFlows, lastTick, random);
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
