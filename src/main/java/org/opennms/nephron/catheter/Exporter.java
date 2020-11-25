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
    private final static Random RANDOM = new Random();

    public final static List<Integer> PROTOCOLS = Arrays.asList(6, 17);
    public final static List<String> APPLICATIONS = generate(200, generateString(15));
    public final static List<String> HOSTS = generate(5, generateString(10));
    public final static List<AddrHost> ADDRESSES = generate(100, () -> new AddrHost(generateInetAddr().get(), generateString(10).get()));

    private final int nodeId;

    private final String foreignSource;
    private final String foreignId;

    private final String location;

    private final FlowGenerator generator;

    private Exporter(final Builder builder,
                     final Instant now) {
        this.nodeId = builder.nodeId;
        this.foreignSource = builder.foreignSource;
        this.foreignId = builder.foreignId;
        this.location = builder.location;

        this.generator = builder.generator.build(now);
    }

    public Collection<FlowDocument> tick(final Instant now) {
        return this.generator.tick(now).stream().map(this::createFlowDocument).collect(Collectors.toList());
    }

    public Collection<FlowDocument> shutdown(final Instant now) {
        return this.generator.shutdown(now).stream().map(this::createFlowDocument).collect(Collectors.toList());
    }

    private FlowDocument createFlowDocument(final FlowReport report) {
        final int protocol = choose(PROTOCOLS);
        final String application = choose(APPLICATIONS);

        final AddrHost srcAddr = choose(ADDRESSES);
        final AddrHost dstAddr = choose(ADDRESSES);

        final InetAddress[] convo = InetAddresses.coerceToInteger(srcAddr.address) < InetAddresses.coerceToInteger(dstAddr.address)
                                    ? new InetAddress[]{srcAddr.address, dstAddr.address}
                                    : new InetAddress[]{dstAddr.address, srcAddr.address};

        final String convoKey = "[\"" + this.location + "\",\"" + protocol + ",\"" + InetAddresses.toAddrString(convo[0]) + "\",\"" + InetAddresses.toAddrString(convo[1]) + "\",\"" + application + "\"]";

        final FlowDocument.Builder flow = FlowDocument.newBuilder();
        flow.setApplication(application);
        flow.setHost(choose(HOSTS));
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

    public static class Builder {
        private int nodeId = 0;

        private String foreignId = "";
        private String foreignSource = "";

        private String location = "Default";

        private FlowGenerator.Builder generator;

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

        public Exporter build(final Instant now) {
            return new Exporter(this, now);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private static class AddrHost {
        public final InetAddress address;
        public final String hostname;

        private AddrHost(final InetAddress address, final String hostname) {
            this.address = Objects.requireNonNull(address);
            this.hostname = Objects.requireNonNull(hostname);
        }
    }

    private static Supplier<String> generateString(final int length) {
        return () -> RANDOM.ints(97, 123)
                           .limit(length)
                           .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                           .toString();
    }

    private static <T> List<T> generate(final int count, final Supplier<T> f) {
        return IntStream.range(0, count)
                        .mapToObj(i -> f.get())
                        .collect(Collectors.toList());
    }

    private static Supplier<Inet4Address> generateInetAddr() {
        return () -> InetAddresses.fromInteger(RANDOM.nextInt());
    }

    private static <T> T choose(final List<T> options) {
        return options.get(RANDOM.nextInt(options.size()));
    }
}
