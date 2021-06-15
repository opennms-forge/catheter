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
import java.util.Objects;
import java.util.Random;

public class Exporter {
    private final int nodeId;
    private final String foreignSource;
    private final String foreignId;
    private final String location;
    private final FlowGenerator generator;
    private final Duration clockOffset;
    private final Random random;
    private final int inputSnmp;
    private final int outputSnmp;

    private Exporter(final Builder builder,
                     final Instant now,
                     final Random random) {
        this.nodeId = builder.nodeId;
        this.foreignSource = builder.foreignSource;
        this.foreignId = builder.foreignId;
        this.location = builder.location;
        this.clockOffset = builder.clockOffset;

        this.random = random;
        this.generator = builder.generator.build(now, random);

        this.inputSnmp = builder.inputSnmp;
        this.outputSnmp = builder.outputSnmp;
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
        return this.generator.tick(now);
    }

    /**
     * Called for the last tick.
     *
     * The last tick happens if either the maximum number of simulation iterations did happen or if stop was called
     * on the simulation.
     */
    public Collection<FlowReport> shutdown(final Instant now) {
        return this.generator.shutdown(now);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Exporter exporter = (Exporter) o;
        return this.nodeId == exporter.nodeId &&
                Objects.equals(this.foreignSource, exporter.foreignSource) &&
                Objects.equals(this.foreignId, exporter.foreignId) &&
                Objects.equals(this.location, exporter.location) &&
                Objects.equals(this.generator, exporter.generator) &&
                Objects.equals(this.inputSnmp, exporter.inputSnmp) &&
                Objects.equals(this.outputSnmp, exporter.outputSnmp) &&
                Objects.equals(this.clockOffset, exporter.clockOffset);
    }

    @Override
    public String toString() {
        return "Exporter{" +
                "nodeId=" + this.nodeId +
                ", foreignSource='" + this.foreignSource + '\'' +
                ", foreignId='" + this.foreignId + '\'' +
                ", location='" + this.location + '\'' +
                ", generator=" + this.generator +
                ", clockOffset=" + this.clockOffset +
                ", inputSnmp=" + this.inputSnmp +
                ", outputSnmp=" + this.outputSnmp +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.nodeId, this.foreignSource, this.foreignId, this.location, this.generator, this.clockOffset, this.random, this.inputSnmp, this.outputSnmp);
    }

    public int getNodeId() {
        return this.nodeId;
    }

    public String getForeignSource() {
        return this.foreignSource;
    }

    public int getInputSnmp() {
        return this.inputSnmp;
    }

    public Duration getClockOffset() {
        return this.clockOffset;
    }

    public int getOutputSnmp() {
        return this.outputSnmp;
    }

    public String getForeignId() {
        return this.foreignId;
    }

    public String getLocation() {
        return this.location;
    }

    public static class Builder {
        private int inputSnmp = 0;
        private int outputSnmp = 0;
        private int nodeId = 0;

        private String foreignId = "";
        private String foreignSource = "";

        private String location = "Default";
        private Duration clockOffset = Duration.ZERO;

        private FlowGenerator.Builder generator = FlowGenerator.builder();

        public Builder withNodeId(final int nodeId) {
            this.nodeId = nodeId;
            return this;
        }

        public Builder withForeignSource(final String foreignSource) {
            this.foreignSource = Objects.requireNonNull(foreignSource);
            return this;
        }

        public Builder withForeignId(final String foreignId) {
            this.foreignId = Objects.requireNonNull(foreignId);
            return this;
        }

        public Builder withGenerator(final FlowGenerator.Builder generator) {
            this.generator = Objects.requireNonNull(generator);
            return this;
        }

        public Builder withLocation(final String location) {
            this.location = Objects.requireNonNull(location);
            return this;
        }

        public Builder withClockOffset(final Duration clockOffset) {
            this.clockOffset = Objects.requireNonNull(clockOffset);
            return this;
        }

        public Builder withBytesPerSecond(final long bytesPerSecond) {
            this.generator.withBytesPerSecond(bytesPerSecond);
            return this;
        }

        public Builder withMinFlowDuration(final Duration minFlowDuration) {
            this.generator.withMinFlowDuration(minFlowDuration);
            return this;
        }

        public Builder withMaxFlowDuration(final Duration maxFlowDuration) {
            this.generator.withMaxFlowDuration(maxFlowDuration);
            return this;
        }

        public Builder withMaxFlowCount(final int maxFlowCount) {
            this.generator.withMaxFlowCount(maxFlowCount);
            return this;
        }

        public Builder withActiveTimeout(final Duration activeTimeout) {
            this.generator.withActiveTimeout(activeTimeout);
            return this;
        }

        public Builder withInputSnmp(final int inputSnmp) {
            this.inputSnmp = inputSnmp;
            return this;
        }

        public Builder withOutputSnmp(final int outputSnmp) {
            this.outputSnmp = outputSnmp;
            return this;
        }

        public Exporter build(final Instant now, final Random random) {
            return new Exporter(this, now, random);
        }
    }
}
