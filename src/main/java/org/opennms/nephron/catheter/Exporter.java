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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.opennms.netmgt.flows.persistence.model.FlowDocument;
import org.opennms.netmgt.flows.persistence.model.Locality;
import org.opennms.netmgt.flows.persistence.model.NodeInfo;

import com.google.common.net.InetAddresses;
import com.google.protobuf.UInt64Value;

public class Exporter {
    private final List<Integer> protocols;
    private final List<String> applications;
    private final List<String> hosts;
    private final List<AddrHost> addresses;

    private final int nodeId;

    private final String foreignSource;
    private final String foreignId;

    private final String location;

    private final FlowGenerator generator;
    private final Duration clockOffset;
    private final Random random;

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

        this.protocols = Arrays.asList(6, 17);
        this.applications = generate(200, generateString(15));
        this.hosts = generate(5, generateString(10));
        this.addresses = generate(100, () -> new AddrHost(generateInetAddr().get(), generateString(10).get()));
    }

    public static Builder builder() {
        return new Builder();
    }

    public Collection<FlowDocument> tick(final Instant now) {
        return this.generator.tick(now.plus(this.clockOffset)).stream().map(this::createFlowDocument).collect(Collectors.toList());
    }

    public Collection<FlowDocument> shutdown(final Instant now) {
        return this.generator.shutdown(now.plus(this.clockOffset)).stream().map(this::createFlowDocument).collect(Collectors.toList());
    }

    private FlowDocument createFlowDocument(final FlowReport report) {
        final int protocol = choose(protocols);
        final String application = choose(applications);

        final AddrHost srcAddr = choose(addresses);
        final AddrHost dstAddr = choose(addresses);

        final InetAddress[] convo = InetAddresses.coerceToInteger(srcAddr.address) < InetAddresses.coerceToInteger(dstAddr.address)
                ? new InetAddress[]{srcAddr.address, dstAddr.address}
                : new InetAddress[]{dstAddr.address, srcAddr.address};

        final String convoKey = "[\"" + this.location + "\",\"" + protocol + ",\"" + InetAddresses.toAddrString(convo[0]) + "\",\"" + InetAddresses.toAddrString(convo[1]) + "\",\"" + application + "\"]";

        final FlowDocument.Builder flow = FlowDocument.newBuilder();
        flow.setApplication(application);
        flow.setHost(choose(hosts));
        flow.setLocation(this.location);
        flow.setDstLocality(Locality.PUBLIC);
        flow.setSrcLocality(Locality.PUBLIC);
        flow.setFlowLocality(Locality.PUBLIC);
        flow.setSrcAddress(InetAddresses.toAddrString(srcAddr.address));
        flow.setDstAddress(InetAddresses.toAddrString(dstAddr.address));
        flow.setSrcHostname(srcAddr.hostname);
        flow.setDstHostname(dstAddr.hostname);
        flow.setFirstSwitched(UInt64Value.of(report.getStart().toEpochMilli()));
        flow.setDeltaSwitched(UInt64Value.of(report.getStart().toEpochMilli()));
        flow.setLastSwitched(UInt64Value.of(report.getStart().toEpochMilli()));
        flow.setNumBytes(UInt64Value.of(report.getBytes()));
        flow.setConvoKey(convoKey);

        final NodeInfo.Builder exporter = NodeInfo.newBuilder();
        exporter.setNodeId(this.nodeId);
        exporter.setForeignSource(this.foreignSource);
        exporter.setForeginId(this.foreignId);
        flow.setExporterNode(exporter);

        return flow.build();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Exporter exporter = (Exporter) o;
        return nodeId == exporter.nodeId &&
                Objects.equals(foreignSource, exporter.foreignSource) &&
                Objects.equals(foreignId, exporter.foreignId) &&
                Objects.equals(location, exporter.location) &&
                Objects.equals(generator, exporter.generator) &&
                Objects.equals(clockOffset, exporter.clockOffset);
    }

    @Override
    public String toString() {
        return "Exporter{" +
                "nodeId=" + nodeId +
                ", foreignSource='" + foreignSource + '\'' +
                ", foreignId='" + foreignId + '\'' +
                ", location='" + location + '\'' +
                ", generator=" + generator +
                ", clockOffset=" + clockOffset +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(protocols, applications, hosts, addresses, nodeId, foreignSource, foreignId, location, generator, clockOffset, random);
    }

    private Supplier<String> generateString(final int length) {
        return () -> random.ints(97, 123)
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    private <T> List<T> generate(final int count, final Supplier<T> f) {
        return IntStream.range(0, count)
                .mapToObj(i -> f.get())
                .collect(Collectors.toList());
    }

    private Supplier<Inet4Address> generateInetAddr() {
        return () -> InetAddresses.fromInteger(random.nextInt());
    }

    private <T> T choose(final List<T> options) {
        return options.get(random.nextInt(options.size()));
    }

    public static class Builder {
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

        public Exporter build(final Instant now, final Random random) {
            return new Exporter(this, now.plus(this.clockOffset), random);
        }
    }

    private static class AddrHost {
        public final InetAddress address;
        public final String hostname;

        private AddrHost(final InetAddress address, final String hostname) {
            this.address = Objects.requireNonNull(address);
            this.hostname = Objects.requireNonNull(hostname);
        }
    }
}
