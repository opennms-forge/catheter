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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.StringReader;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.persistence.jaxb.MarshallerProperties;
import org.opennms.nephron.catheter.json.ExporterJson;
import org.opennms.nephron.catheter.json.SimulationJson;
import org.opennms.netmgt.flows.persistence.model.FlowDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Simulation {
    private static final Logger LOG = LoggerFactory.getLogger(Simulation.class);
    private final String bootstrapServers;
    private final String flowTopic;
    private final Duration tickMs;
    private final boolean realtime;
    private final Instant startTime;
    private final List<Exporter> exporters;
    private Thread thread;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Duration elapsedTime = Duration.ZERO;
    private long flowsSent = 0;
    private long bytesSent = 0;
    private final Random random = new Random();
    private long maxIterations = 0;

    private Simulation(final Builder builder) {
        this.bootstrapServers = Objects.requireNonNull(builder.bootstrapServers);
        this.flowTopic = Objects.requireNonNull(builder.flowTopic);
        this.tickMs = Objects.requireNonNull(builder.tickMs);
        this.realtime = builder.realtime;
        this.startTime = Instant.ofEpochMilli(builder.startTime != null ? builder.startTime.toEpochMilli() : Instant.now().toEpochMilli() / builder.tickMs.toMillis() * builder.tickMs.toMillis());
        this.random.setSeed(builder.seed);
        this.exporters = builder.exporters.stream().map(e -> e.build(this.startTime, random)).collect(Collectors.toList());
    }

    public static Simulation fromFile(final File file) throws JAXBException, FileNotFoundException {
        return fromSource(new StreamSource(new FileReader(file)));

    }

    public static Simulation fromJson(final String json) throws JAXBException {
        return fromSource(new StreamSource(new StringReader(json)));
    }

    private static Simulation fromSource(final Source source) throws JAXBException {
        final Unmarshaller unmarshaller = JAXBContext.newInstance(SimulationJson.class).createUnmarshaller();
        unmarshaller.setProperty(MarshallerProperties.MEDIA_TYPE, "application/json");
        final SimulationJson simulationJson = unmarshaller.unmarshal(source, SimulationJson.class).getValue();

        final List<Exporter.Builder> exporterBuilders = new ArrayList<>();

        for(final ExporterJson exporterJson : simulationJson.getExporters()) {
            final FlowGenerator.Builder flowGeneratorBuilder = FlowGenerator.builder()
                    .withMaxFlowCount(exporterJson.getFlowGenerator().getMaxFlowCount())
                    .withMinFlowDuration(Duration.ofMillis(exporterJson.getFlowGenerator().getMinFlowDurationMs()))
                    .withMaxFlowDuration(Duration.ofMillis(exporterJson.getFlowGenerator().getMaxFlowDurationMs()))
                    .withActiveTimeout(Duration.ofMillis(exporterJson.getFlowGenerator().getActiveTimeoutMs()))
                    .withBytesPerSecond(exporterJson.getFlowGenerator().getBytesPerSecond());

            exporterBuilders.add(Exporter.builder()
                                    .withForeignId(exporterJson.getForeignId())
                                    .withForeignSource(exporterJson.getForeignSource())
                                    .withNodeId(exporterJson.getNodeId())
                                    .withLocation(exporterJson.getLocation())
                                    .withInputSnmp(exporterJson.getInputSnmp())
                                    .withOutputSnmp(exporterJson.getOutputSnmp())
                                    .withGenerator(flowGeneratorBuilder)
                                    .withClockOffset(Duration.ofMillis(exporterJson.getClockOffsetMs())));
        }

        return Simulation.builder()
            .withStartTime(simulationJson.getStartTime())
            .withSeed(simulationJson.getSeed())
            .withBootstrapServers(simulationJson.getBootstrapServers())
            .withTickMs(Duration.ofMillis(simulationJson.getTickMs()))
            .withFlowTopic(simulationJson.getFlowTopic())
            .withRealtime(simulationJson.getRealtime())
            .withExporters(exporterBuilders).build();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Simulation that = (Simulation) o;
        return this.realtime == that.realtime &&
                Objects.equals(this.bootstrapServers, that.bootstrapServers) &&
                Objects.equals(this.flowTopic, that.flowTopic) &&
                Objects.equals(this.tickMs, that.tickMs) &&
                Objects.equals(this.startTime, that.startTime) &&
                Objects.equals(this.exporters, that.exporters);
    }

    @Override
    public String toString() {
        return "Simulation{" +
                "bootstrapServers='" + this.bootstrapServers + '\'' +
                ", flowTopic='" + this.flowTopic + '\'' +
                ", tickMs=" + this.tickMs +
                ", realtime=" + this.realtime +
                ", startTime=" + this.startTime +
                ", exporters=" + this.exporters +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.bootstrapServers, this.flowTopic, this.tickMs, this.realtime, this.startTime, this.exporters, this.thread, this.running, this.elapsedTime, this.flowsSent, this.bytesSent, this.random, this.maxIterations);
    }

    public static Builder builder() {
        return new Builder();
    }

    public void start() {
        start(0);
    }

    void start(final long maxIterations) {
        this.maxIterations = maxIterations;
        if (!this.running.get()) {
            this.running.set(true);
            this.thread = new Thread(this::run);
            this.thread.start();
        }
    }

    private void run() {
        this.elapsedTime = Duration.ZERO;
        this.flowsSent = 0;
        this.bytesSent = 0;

        final Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServers);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        KafkaProducer<String, byte[]> kafkaProducer = new KafkaProducer<>(producerProps);

        Instant now = this.startTime;

        while (this.running.get()) {
            now = now.plus(this.tickMs);
            this.elapsedTime = Duration.between(this.startTime, now);

            if (this.maxIterations > 0) {
                this.maxIterations--;
                if (this.maxIterations == 0) {
                    this.running.set(false);
                }
            }

            if (this.realtime) {
                final Duration timeToSleep = Duration.between(Instant.now(), now);
                if (!timeToSleep.isNegative()) {
                    try {
                        LOG.trace("Sleeping for {} ...", timeToSleep);
                        Thread.sleep(timeToSleep.toMillis());
                    } catch (InterruptedException e) {
                        LOG.warn("Simulation: exception while Thread.sleep()", e);
                    }
                }
            }


            for (final Exporter exporter : this.exporters) {
                sendFlowDocuments(kafkaProducer, exporter.tick(now));
            }
        }

        LOG.debug("Simulation: shutting down {} exporters", this.exporters.size());

        for (final Exporter exporter : this.exporters) {
            sendFlowDocuments(kafkaProducer, exporter.shutdown(now));
        }

        kafkaProducer.close();
    }

    private void sendFlowDocuments(final KafkaProducer<String, byte[]> kafkaProducer, final Collection<FlowDocument> flowDocuments) {
        this.flowsSent += flowDocuments.size();
        for (final FlowDocument flowDocument : flowDocuments) {
            this.bytesSent += flowDocument.getNumBytes().getValue();
            kafkaProducer.send(new ProducerRecord<>(this.flowTopic, flowDocument.toByteArray()), (metadata, exception) -> {
                if (exception != null) {
                    LOG.warn("Simulation: error sending flow document to Kafka topic", exception);
                }
            });
        }
        if (!flowDocuments.isEmpty()) {
            LOG.debug("Simulation: sent {} flow documents to Kafka topic '{}'", flowDocuments.size(), this.flowTopic);
        }
    }

    public void join() throws InterruptedException {
        if (this.thread != null) {
            this.thread.join();
        }
    }

    public void stop() {
        if (this.running.get()) {
            this.running.set(false);
        }
    }

    public Duration getElapsedTime() {
        return this.elapsedTime;
    }

    public long getFlowsSent() {
        return this.flowsSent;
    }

    public long getBytesSent() {
        return this.bytesSent;
    }

    public static class Builder {
        public String flowTopic;
        public long seed = new Random().nextLong();
        private String bootstrapServers;
        private Duration tickMs = Duration.ofMillis(250);
        private boolean realtime;
        private Instant startTime;
        private final List<Exporter.Builder> exporters = new ArrayList<>();

        private Builder() {
        }

        public Builder withBootstrapServers(final String bootstrapServers) {
            this.bootstrapServers = Objects.requireNonNull(bootstrapServers);
            return this;
        }

        public Builder withFlowTopic(final String flowTopic) {
            this.flowTopic = flowTopic;
            return this;
        }

        public Builder withTickMs(final Duration tickMs) {
            this.tickMs = Objects.requireNonNull(tickMs);
            return this;
        }

        public Builder withRealtime(final boolean realtime) {
            this.realtime = realtime;
            return this;
        }

        public Builder withStartTime(final Instant startTime) {
            this.startTime = Objects.requireNonNull(startTime);
            return this;
        }

        public Simulation build() {
            return new Simulation(this);
        }

        public Builder withExporters(final Exporter.Builder... builders) {
            this.exporters.addAll(Arrays.asList(builders));
            return this;
        }

        public Builder withExporters(final Collection<Exporter.Builder> builders) {
            this.exporters.addAll(builders);
            return this;
        }

        public Builder withSeed(final long seed) {
            this.seed = seed;
            return this;
        }
    }
}
