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
import java.util.Objects;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;

public class Flow {
    private final Instant start;
    private final long bytesPerSecond;
    private Instant reported;
    private long bytes;

    public Flow(final Instant start,
                final long bytesPerSecond) {
        this.start = Objects.requireNonNull(start);
        this.bytesPerSecond = bytesPerSecond;
        this.reported = start;
        this.bytes = 0;
    }

    public Instant getStart() {
        return this.start;
    }

    public boolean checkTimeout(final Instant now, final Duration activeTimeout) {
        return !this.reported.plus(activeTimeout).isAfter(now);
    }

    protected FlowReport report(final Instant now) {
        // Create report of current stats
        // Report the real flow end if the flow has ended
        final FlowReport report = new FlowReport(this.reported,
                now,
                this.bytes);

        // Reset the stats
        this.reported = now;
        this.bytes = 0;

        return report;
    }

    protected void transmit(final long bytes) {
        this.bytes += bytes;
    }

    public long getBytesPerSecond() {
        return this.bytesPerSecond;
    }

    @VisibleForTesting
    public long getBytes() { return bytes; }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Flow flow = (Flow) o;
        return this.bytes == flow.bytes &&
                Objects.equals(this.start, flow.start) &&
                Objects.equals(this.bytesPerSecond, flow.bytesPerSecond) &&
                Objects.equals(this.reported, flow.reported);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.start, this.reported, this.bytes, this.bytesPerSecond);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("start", this.start)
                .add("lastReported", this.reported)
                .add("bytes", this.bytes)
                .add("bytesPerSecond", this.bytesPerSecond)
                .toString();
    }
}
