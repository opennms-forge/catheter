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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlRootElement(name = "simulation")
public class SimulationJson {
    private String bootstrapServers = "";

    private String flowTopic = "";

    private long tickMs = 250;

    private boolean realtime = false;

    private Instant startTime = Instant.now();

    private List<ExporterJson> exporters = new ArrayList<>();

    private long seed = new Random().nextLong();

    public SimulationJson() {
    }

    @XmlElement(name = "bootStrapServers")
    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(final String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    @XmlElement(name = "flowTopic")
    public String getFlowTopic() {
        return flowTopic;
    }

    public void setFlowTopic(final String flowTopic) {
        this.flowTopic = flowTopic;
    }

    @XmlElement(name = "tickMs")
    public long getTickMs() {
        return tickMs;
    }

    public void setTickMs(final long tickMs) {
        this.tickMs = tickMs;
    }

    @XmlElement(name = "realtime")
    public boolean getRealtime() {
        return realtime;
    }

    public void setRealtime(final boolean realtime) {
        this.realtime = realtime;
    }

    @XmlElement(name = "startTime")
    @XmlJavaTypeAdapter(InstantXmlAdapter.class)
    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(final Instant startTime) {
        this.startTime = startTime;
    }

    public List<ExporterJson> getExporters() {
        return exporters;
    }

    public void setExporters(final List<ExporterJson> exporters) {
        this.exporters = exporters;
    }

    @XmlElement(name = "seed")
    public long getSeed() {
        return seed;
    }

    public void setSeed(final long seed) {
        this.seed = seed;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimulationJson that = (SimulationJson) o;
        return tickMs == that.tickMs &&
                realtime == that.realtime &&
                seed == that.seed &&
                Objects.equals(bootstrapServers, that.bootstrapServers) &&
                Objects.equals(flowTopic, that.flowTopic) &&
                Objects.equals(startTime, that.startTime) &&
                Objects.equals(exporters, that.exporters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bootstrapServers, flowTopic, tickMs, realtime, startTime, exporters, seed);
    }

    @Override
    public String toString() {
        return "SimulationJson{" +
                "bootstrapServers='" + bootstrapServers + '\'' +
                ", flowTopic='" + flowTopic + '\'' +
                ", tickMs=" + tickMs +
                ", realtime=" + realtime +
                ", startTime=" + startTime +
                ", exporters=" + exporters +
                ", seed=" + seed +
                '}';
    }
}
