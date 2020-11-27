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
    private long minFlowDuration = 2000;
    private long maxFlowDuration = 20000;
    private long activeTimeout = 1000;

    public FlowGeneratorJson() {
    }

    @XmlElement(name = "bytesPerSecond")
    public long getBytesPerSecond() {
        return bytesPerSecond;
    }

    public void setBytesPerSecond(final long bytesPerSecond) {
        this.bytesPerSecond = bytesPerSecond;
    }

    @XmlElement(name = "maxFlowCount")
    public int getMaxFlowCount() {
        return maxFlowCount;
    }

    public void setMaxFlowCount(final int maxFlowCount) {
        this.maxFlowCount = maxFlowCount;
    }

    @XmlElement(name = "minFlowDuration")
    public long getMinFlowDuration() {
        return minFlowDuration;
    }

    public void setMinFlowDuration(final long minFlowDuration) {
        this.minFlowDuration = minFlowDuration;
    }

    @XmlElement(name = "maxFlowDuration")
    public long getMaxFlowDuration() {
        return maxFlowDuration;
    }

    public void setMaxFlowDuration(final long maxFlowDuration) {
        this.maxFlowDuration = maxFlowDuration;
    }

    @XmlElement(name = "activeTimeout")
    public long getActiveTimeout() {
        return activeTimeout;
    }

    public void setActiveTimeout(final long activeTimeout) {
        this.activeTimeout = activeTimeout;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlowGeneratorJson that = (FlowGeneratorJson) o;
        return bytesPerSecond == that.bytesPerSecond &&
                maxFlowCount == that.maxFlowCount &&
                minFlowDuration == that.minFlowDuration &&
                maxFlowDuration == that.maxFlowDuration &&
                activeTimeout == that.activeTimeout;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bytesPerSecond, maxFlowCount, minFlowDuration, maxFlowDuration, activeTimeout);
    }

    @Override
    public String toString() {
        return "FlowGeneratorJson{" +
                "bytesPerSecond=" + bytesPerSecond +
                ", maxFlowCount=" + maxFlowCount +
                ", minFlowDuration=" + minFlowDuration +
                ", maxFlowDuration=" + maxFlowDuration +
                ", activeTimeout=" + activeTimeout +
                '}';
    }
}
