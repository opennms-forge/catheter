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

import com.google.common.base.MoreObjects;

public class FlowReport {

    private final Instant start;
    private final Instant end;

    private final long bytes;

    public FlowReport(final Instant start,
                      final Instant end,
                      final long bytes) {
        this.start = Objects.requireNonNull(start);
        this.end = Objects.requireNonNull(end);
        this.bytes = bytes;
    }

    public Instant getStart() {
        return this.start;
    }

    public Instant getEnd() {
        return this.end;
    }

    public long getBytes() {
        return this.bytes;
    }

    public Duration getDuration() {
        return Duration.between(this.start, this.end);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlowReport that = (FlowReport) o;
        return bytes == that.bytes &&
                Objects.equals(start, that.start) &&
                Objects.equals(end, that.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end, bytes);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("start", this.start)
                .add("end", this.end)
                .add("bytes", this.bytes)
                .toString();
    }
}
