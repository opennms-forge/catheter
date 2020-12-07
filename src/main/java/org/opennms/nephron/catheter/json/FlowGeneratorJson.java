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

package org.opennms.nephron.catheter.json;

import java.util.Objects;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "flowGenerator")
public class FlowGeneratorJson {
    private long bytesPerSecond = 1;
    private int maxFlowCount = 10;
    private long minFlowDurationMs = 2000;
    private long maxFlowDurationMs = 20000;
    private long activeTimeoutMs = 1000;

    public FlowGeneratorJson() {
    }

    @XmlElement(name = "bytesPerSecond")
    public long getBytesPerSecond() {
        return this.bytesPerSecond;
    }

    public void setBytesPerSecond(final long bytesPerSecond) {
        this.bytesPerSecond = bytesPerSecond;
    }

    @XmlElement(name = "maxFlowCount")
    public int getMaxFlowCount() {
        return this.maxFlowCount;
    }

    public void setMaxFlowCount(final int maxFlowCount) {
        this.maxFlowCount = maxFlowCount;
    }

    @XmlElement(name = "minFlowDurationMs")
    public long getMinFlowDurationMs() {
        return this.minFlowDurationMs;
    }

    public void setMinFlowDurationMs(final long minFlowDurationMs) {
        this.minFlowDurationMs = minFlowDurationMs;
    }

    @XmlElement(name = "maxFlowDurationMs")
    public long getMaxFlowDurationMs() {
        return this.maxFlowDurationMs;
    }

    public void setMaxFlowDurationMs(final long maxFlowDurationMs) {
        this.maxFlowDurationMs = maxFlowDurationMs;
    }

    @XmlElement(name = "activeTimeoutMs")
    public long getActiveTimeoutMs() {
        return this.activeTimeoutMs;
    }

    public void setActiveTimeoutMs(final long activeTimeoutMs) {
        this.activeTimeoutMs = activeTimeoutMs;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final FlowGeneratorJson that = (FlowGeneratorJson) o;
        return this.bytesPerSecond == that.bytesPerSecond &&
                this.maxFlowCount == that.maxFlowCount &&
                this.minFlowDurationMs == that.minFlowDurationMs &&
                this.maxFlowDurationMs == that.maxFlowDurationMs &&
                this.activeTimeoutMs == that.activeTimeoutMs;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.bytesPerSecond, this.maxFlowCount, this.minFlowDurationMs, this.maxFlowDurationMs, this.activeTimeoutMs);
    }

    @Override
    public String toString() {
        return "FlowGeneratorJson{" +
                "bytesPerSecond=" + this.bytesPerSecond +
                ", maxFlowCount=" + this.maxFlowCount +
                ", minFlowDurationMs=" + this.minFlowDurationMs +
                ", maxFlowDurationMs=" + this.maxFlowDurationMs +
                ", activeTimeoutMs=" + this.activeTimeoutMs +
                '}';
    }
}
