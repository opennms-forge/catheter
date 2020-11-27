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

@XmlRootElement(name = "exporter")
public class ExporterJson {
    private int nodeId = 0;
    private String foreignSource = "";
    private String foreignId = "";
    private String location = "";
    private long clockOffset = 0;
    private FlowGeneratorJson flowGenerator;

    public ExporterJson() {
    }

    @XmlElement(name = "nodeId")
    public int getNodeId() {
        return nodeId;
    }

    public void setNodeId(final int nodeId) {
        this.nodeId = nodeId;
    }

    @XmlElement(name = "foreignSource")
    public String getForeignSource() {
        return foreignSource;
    }

    public void setForeignSource(final String foreignSource) {
        this.foreignSource = foreignSource;
    }

    @XmlElement(name = "foreignId")
    public String getForeignId() {
        return foreignId;
    }

    public void setForeignId(final String foreignId) {
        this.foreignId = foreignId;
    }

    @XmlElement(name = "location")
    public String getLocation() {
        return location;
    }

    public void setLocation(final String location) {
        this.location = location;
    }

    @XmlElement(name = "clockOffset")
    public long getClockOffset() {
        return clockOffset;
    }

    public void setClockOffset(final long clockOffset) {
        this.clockOffset = clockOffset;
    }

    @XmlElement(name = "flowGenerator")
    public FlowGeneratorJson getFlowGenerator() {
        return flowGenerator;
    }

    public void setFlowGenerator(final FlowGeneratorJson flowGenerator) {
        this.flowGenerator = flowGenerator;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExporterJson that = (ExporterJson) o;
        return nodeId == that.nodeId &&
                clockOffset == that.clockOffset &&
                Objects.equals(foreignSource, that.foreignSource) &&
                Objects.equals(foreignId, that.foreignId) &&
                Objects.equals(location, that.location) &&
                Objects.equals(flowGenerator, that.flowGenerator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, foreignSource, foreignId, location, clockOffset, flowGenerator);
    }

    @Override
    public String toString() {
        return "ExporterJson{" +
                "nodeId=" + nodeId +
                ", foreignSource='" + foreignSource + '\'' +
                ", foreignId='" + foreignId + '\'' +
                ", location='" + location + '\'' +
                ", clockOffset=" + clockOffset +
                ", flowGenerator=" + flowGenerator +
                '}';
    }
}
