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
    private long clockOffsetMs = 0;
    private FlowGeneratorJson flowGenerator;
    private int inputSnmp = 0;
    private int outputSnmp = 0;

    public ExporterJson() {
    }

    @XmlElement(name = "nodeId")
    public int getNodeId() {
        return this.nodeId;
    }

    public void setNodeId(final int nodeId) {
        this.nodeId = nodeId;
    }

    @XmlElement(name = "foreignSource")
    public String getForeignSource() {
        return this.foreignSource;
    }

    public void setForeignSource(final String foreignSource) {
        this.foreignSource = foreignSource;
    }

    @XmlElement(name = "foreignId")
    public String getForeignId() {
        return this.foreignId;
    }

    public void setForeignId(final String foreignId) {
        this.foreignId = foreignId;
    }

    @XmlElement(name = "location")
    public String getLocation() {
        return this.location;
    }

    public void setLocation(final String location) {
        this.location = location;
    }

    @XmlElement(name = "clockOffsetMs")
    public long getClockOffsetMs() {
        return this.clockOffsetMs;
    }

    public void setClockOffsetMs(final long clockOffsetMs) {
        this.clockOffsetMs = clockOffsetMs;
    }

    @XmlElement(name = "flowGenerator")
    public FlowGeneratorJson getFlowGenerator() {
        return this.flowGenerator;
    }

    public void setFlowGenerator(final FlowGeneratorJson flowGenerator) {
        this.flowGenerator = flowGenerator;
    }

    @XmlElement(name = "inputSnmp")
    public int getInputSnmp() {
        return this.inputSnmp;
    }

    public void setInputSnmp(final int inputSnmp) {
        this.inputSnmp = inputSnmp;
    }

    @XmlElement(name = "outputSnmp")
    public int getOutputSnmp() {
        return this.outputSnmp;
    }

    public void setOutputSnmp(final int outputSnmp) {
        this.outputSnmp = outputSnmp;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ExporterJson that = (ExporterJson) o;
        return this.nodeId == that.nodeId &&
                this.clockOffsetMs == that.clockOffsetMs &&
                Objects.equals(this.foreignSource, that.foreignSource) &&
                Objects.equals(this.foreignId, that.foreignId) &&
                Objects.equals(this.location, that.location) &&
                Objects.equals(this.inputSnmp, that.inputSnmp) &&
                Objects.equals(this.outputSnmp, that.outputSnmp) &&
                Objects.equals(this.flowGenerator, that.flowGenerator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.nodeId, this.foreignSource, this.foreignId, this.location, this.clockOffsetMs, this.flowGenerator);
    }

    @Override
    public String toString() {
        return "ExporterJson{" +
                "nodeId=" + this.nodeId +
                ", foreignSource='" + this.foreignSource + '\'' +
                ", foreignId='" + this.foreignId + '\'' +
                ", location='" + this.location + '\'' +
                ", clockOffsetMs=" + this.clockOffsetMs +
                ", flowGenerator=" + this.flowGenerator +
                ", inputSnmp=" + this.inputSnmp +
                ", outputSnmp=" + this.outputSnmp +
                '}';
    }
}
