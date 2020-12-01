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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.eclipse.persistence.jaxb.MarshallerProperties;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.opennms.nephron.NephronOptions;
import org.opennms.nephron.catheter.json.ExporterJson;
import org.opennms.nephron.catheter.json.FlowGeneratorJson;
import org.opennms.nephron.catheter.json.SimulationJson;
import org.opennms.nephron.coders.KafkaInputFlowDeserializer;
import org.opennms.netmgt.flows.persistence.model.FlowDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.KafkaContainer;

import com.google.common.collect.ImmutableMap;
import static org.awaitility.Awaitility.*;

public class CatheterIT {
    private static final Logger LOG = LoggerFactory.getLogger(CatheterIT.class);

    @Rule
    public KafkaContainer kafka = new KafkaContainer();

    @Before
    public void before() {
        createTopics(NephronOptions.DEFAULT_FLOW_SOURCE_TOPIC);
    }

    @Test
    public void testMainMethod() throws Exception {
        final SimulationJson simulationJson = new SimulationJson();
        simulationJson.setBootstrapServers(kafka.getBootstrapServers());
        simulationJson.setFlowTopic(NephronOptions.DEFAULT_FLOW_SOURCE_TOPIC);
        simulationJson.setRealtime(true);
        simulationJson.setStartTime(Instant.now());

        final FlowGeneratorJson flowGeneratorJson1 = new FlowGeneratorJson();
        flowGeneratorJson1.setActiveTimeoutMs(1000);
        flowGeneratorJson1.setBytesPerSecond(1000_000);
        flowGeneratorJson1.setMaxFlowCount(10);
        flowGeneratorJson1.setMinFlowDurationMs(1000);
        flowGeneratorJson1.setMaxFlowDurationMs(20000);

        final ExporterJson exporterJson1 = new ExporterJson();
        exporterJson1.setForeignSource("foreignSource1");
        exporterJson1.setForeignId("foreignId1");
        exporterJson1.setNodeId(1);
        exporterJson1.setClockOffsetMs(10);
        exporterJson1.setFlowGenerator(flowGeneratorJson1);
        exporterJson1.setLocation("Default");

        simulationJson.setExporters(Arrays.asList(exporterJson1));

        final Marshaller marshaller = JAXBContext.newInstance(SimulationJson.class).createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(MarshallerProperties.MEDIA_TYPE, "application/json");
        marshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, true);

        final File tempFile = File.createTempFile("test-", ".json");
        tempFile.deleteOnExit();

        // write JSON file
        marshaller.marshal(simulationJson, tempFile);
        // run main with JSON file argument
        Catheter.main(tempFile.getAbsolutePath());
        // setup consumer
        final KafkaConsumer<String, FlowDocument> kafkaConsumer = createConsumer();
        // check whether data arrive...
        await().pollDelay(Duration.ofSeconds(1)).atMost(Duration.ofMinutes(1)).until(() -> kafkaConsumer.poll(250).count()>0);
        // close the consumer
        kafkaConsumer.close();
    }

    @Test
    public void testJsonHandling() throws Exception {
        final Simulation expected = Simulation.builder()
                .withBootstrapServers("bootstrapServers")
                .withFlowTopic("flowTopic")
                .withRealtime(true)
                .withStartTime(Instant.parse("2020-11-27T09:16:31.122Z"))
                .withTickMs(Duration.ofMillis(250))
                .withExporters(
                        Exporter.builder()
                                .withNodeId(1)
                                .withForeignSource("foreignSource1")
                                .withForeignId("foreignId1")
                                .withClockOffset(Duration.ofSeconds(10))
                                .withLocation("Default")
                                .withGenerator(FlowGenerator.builder()
                                        .withBytesPerSecond(1000_000L)
                                        .withMaxFlowCount(10)
                                        .withActiveTimeout(Duration.ofSeconds(1))
                                        .withMinFlowDuration(Duration.ofSeconds(1))
                                        .withMaxFlowDuration(Duration.ofSeconds(20))),
                        Exporter.builder()
                                .withNodeId(2)
                                .withForeignSource("foreignSource2")
                                .withForeignId("foreignId2")
                                .withClockOffset(Duration.ofSeconds(-10))
                                .withLocation("Minion")
                                .withGenerator(FlowGenerator.builder()
                                        .withBytesPerSecond(1000_000L)
                                        .withMaxFlowCount(10)
                                        .withActiveTimeout(Duration.ofSeconds(1))
                                        .withMinFlowDuration(Duration.ofSeconds(2))
                                        .withMaxFlowDuration(Duration.ofSeconds(15)))
                )
                .withSeed(1606468048782L)
                .build();

        // check whether loaded file and expected simulation instance is equal
        assertThat(Simulation.fromFile(new File("src/test/resources/simulation.json")), is(expected));
    }

    @Test
    public void testSeed() throws Exception {
        // create random seed
        long seed = new Random().nextLong();

        final Instant now = Instant.ofEpochMilli(1_500_000_000_000L);

        final KafkaConsumer<String, FlowDocument> kafkaConsumer = createConsumer();

        final Simulation s1 = createSimulationWithSeed(now, seed);

        // run first simulation with seed
        s1.start(20);
        s1.join();

        final AtomicLong received1 = new AtomicLong();
        final List<FlowDocument> flows1 = new ArrayList<>();

        // wait till all data arrived
        await().pollDelay(Duration.ofSeconds(1)).atMost(Duration.ofMinutes(1)).until(() -> {
            final ConsumerRecords<String, FlowDocument> records = kafkaConsumer.poll(1000);
            received1.addAndGet(records.count());

            for(final ConsumerRecord<String, FlowDocument> record : records) {
                flows1.add(record.value());
            }

            return received1.get() >= s1.getFlowsSent();
        });

        final Simulation s2 = createSimulationWithSeed(now, seed);

        // run second simulation with same seed
        s2.start(20);
        s2.join();

        final AtomicLong received2 = new AtomicLong();
        final List<FlowDocument> flows2 = new ArrayList<>();

        // wait till all data arrived
        await().pollDelay(Duration.ofSeconds(1)).atMost(Duration.ofMinutes(1)).until(() -> {
            final ConsumerRecords<String, FlowDocument> records = kafkaConsumer.poll(1000);
            received2.addAndGet(records.count());

            for(final ConsumerRecord<String, FlowDocument> record : records) {
                flows2.add(record.value());
            }

            return received2.get() >= s2.getFlowsSent();
        });

        // check whether the results ot the two simulation runs are ther same
        assertThat(s1.getBytesSent(), is(s2.getBytesSent()));
        assertThat(s1.getElapsedTime(), is(s2.getElapsedTime()));
        assertThat(s1.getFlowsSent(), is(s2.getFlowsSent()));
        Assert.assertEquals(flows1, flows2);
        // close the consumer
        kafkaConsumer.close();
    }

    private Simulation createSimulationWithSeed(final Instant now, final long seed) {
        return Simulation.builder()
                .withBootstrapServers(kafka.getBootstrapServers())
                .withFlowTopic(NephronOptions.DEFAULT_FLOW_SOURCE_TOPIC)
                .withRealtime(false)
                .withStartTime(now)
                .withTickMs(Duration.ofMillis(50))
                .withExporters(
                        Exporter.builder()
                                .withNodeId(1)
                                .withForeignSource("exporters")
                                .withForeignId("test1")
                                .withClockOffset(Duration.ofSeconds(-10))
                                .withGenerator(FlowGenerator.builder()
                                        .withBytesPerSecond(750_000L)
                                        .withMaxFlowCount(10)
                                        .withActiveTimeout(Duration.ofSeconds(2))
                                        .withMinFlowDuration(Duration.ofSeconds(1))
                                        .withMaxFlowDuration(Duration.ofSeconds(20))),
                        Exporter.builder()
                                .withNodeId(2)
                                .withForeignSource("exporters")
                                .withForeignId("test2")
                                .withClockOffset(Duration.ofSeconds(10))
                                .withGenerator(FlowGenerator.builder()
                                        .withBytesPerSecond(250_000L)
                                        .withMaxFlowCount(10)
                                        .withActiveTimeout(Duration.ofSeconds(1))
                                        .withMinFlowDuration(Duration.ofSeconds(2))
                                        .withMaxFlowDuration(Duration.ofSeconds(15)))
                )
                .withSeed(seed)
                .build();
    }

    @Test
    public void testRealtime() {
        final Simulation simulation = Simulation.builder()
                .withBootstrapServers(kafka.getBootstrapServers())
                .withFlowTopic(NephronOptions.DEFAULT_FLOW_SOURCE_TOPIC)
                .withRealtime(true)
                .withTickMs(Duration.ofMillis(250))
                .withExporters(
                        Exporter.builder()
                                .withNodeId(1)
                                .withForeignSource("exporters")
                                .withForeignId("test1")
                                .withClockOffset(Duration.ofSeconds(-10))
                                .withGenerator(FlowGenerator.builder()
                                        .withBytesPerSecond(750_000L)
                                        .withMaxFlowCount(10)
                                        .withActiveTimeout(Duration.ofSeconds(2))
                                        .withMinFlowDuration(Duration.ofSeconds(1))
                                        .withMaxFlowDuration(Duration.ofSeconds(20))),
                        Exporter.builder()
                                .withNodeId(2)
                                .withForeignSource("exporters")
                                .withForeignId("test2")
                                .withClockOffset(Duration.ofSeconds(10))
                                .withGenerator(FlowGenerator.builder()
                                        .withBytesPerSecond(250_000L)
                                        .withMaxFlowCount(10)
                                        .withActiveTimeout(Duration.ofSeconds(1))
                                        .withMinFlowDuration(Duration.ofSeconds(2))
                                        .withMaxFlowDuration(Duration.ofSeconds(15)))
                        )
                .build();

        runSimulation(simulation, Duration.ofSeconds(5));
    }

    @Test
    public void testNonRealtime() {
        final Instant now = Instant.ofEpochMilli(1_500_000_000_000L);

        final Simulation simulation = Simulation.builder()
                .withBootstrapServers(kafka.getBootstrapServers())
                .withFlowTopic(NephronOptions.DEFAULT_FLOW_SOURCE_TOPIC)
                .withRealtime(false)
                .withTickMs(Duration.ofMillis(250))
                .withStartTime(now)
                .withExporters(
                        Exporter.builder()
                                .withNodeId(1)
                                .withForeignSource("exporters")
                                .withForeignId("test1")
                                .withClockOffset(Duration.ofSeconds(-10))
                                .withGenerator(FlowGenerator.builder()
                                        .withBytesPerSecond(750_000L)
                                        .withMaxFlowCount(10)
                                        .withActiveTimeout(Duration.ofSeconds(2))
                                        .withMinFlowDuration(Duration.ofSeconds(1))
                                        .withMaxFlowDuration(Duration.ofSeconds(20))),
                        Exporter.builder()
                                .withNodeId(2)
                                .withForeignSource("exporters")
                                .withForeignId("test2")
                                .withClockOffset(Duration.ofSeconds(10))
                                .withGenerator(FlowGenerator.builder()
                                        .withBytesPerSecond(250_000L)
                                        .withMaxFlowCount(10)
                                        .withActiveTimeout(Duration.ofSeconds(1))
                                        .withMinFlowDuration(Duration.ofSeconds(2))
                                        .withMaxFlowDuration(Duration.ofSeconds(15)))
                )
                .build();

        runSimulation(simulation, Duration.ofSeconds(5));
    }

    public void runSimulation(final Simulation simulation, final Duration duration) {
        createTopics(NephronOptions.DEFAULT_FLOW_SOURCE_TOPIC);

        simulation.start();

        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        simulation.stop();

        try {
            simulation.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        final long rateSent = (long) ((double) simulation.getBytesSent() / (double) simulation.getElapsedTime().toMillis() * 1000.0);

        LOG.debug("Simulation took {} ms", simulation.getElapsedTime().toMillis());
        LOG.debug("Simulation reported {} flows", simulation.getFlowsSent());
        LOG.debug("Simulation reported {} bytes in total", simulation.getBytesSent());
        LOG.debug("Simulation rate was {} byte/sec", rateSent);

        assertThat(rateSent, is(1000000L));

        final KafkaConsumer<String, FlowDocument> kafkaConsumer = createConsumer();

        final AtomicLong flowsReceived = new AtomicLong();
        final AtomicLong bytesReceived = new AtomicLong();

        await().pollDelay(Duration.ofSeconds(1)).atMost(Duration.ofMinutes(1)).until(() -> {
            final ConsumerRecords<String, FlowDocument> records = kafkaConsumer.poll(1000);
            flowsReceived.addAndGet(records.count());

            for(final ConsumerRecord<String, FlowDocument> record : records) {
                bytesReceived.addAndGet(record.value().getNumBytes().getValue());
            }

            return flowsReceived.get() >= simulation.getFlowsSent();
        });

        final long rateReceived = (long) ((double) bytesReceived.get() / (double) simulation.getElapsedTime().toMillis() * 1000.0);

        LOG.debug("Kafka received {} flows", flowsReceived);
        LOG.debug("Kafka received {} bytes in total", bytesReceived);
        LOG.debug("Kafka rate is {} byte/sec", rateReceived);

        assertThat(bytesReceived.get(), is(simulation.getBytesSent()));
        assertThat(rateReceived, is(1000000L));

        kafkaConsumer.close();
    }

    private void createTopics(String ... topics) {
        final List<NewTopic> newTopics =
                Arrays.stream(topics)
                        .map(topic -> new NewTopic(topic, 1, (short) 1))
                        .collect(Collectors.toList());
        try (final AdminClient admin = AdminClient.create(ImmutableMap.<String,Object>builder()
                .put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers())
                .build())) {
            admin.createTopics(newTopics);
        }
    }

    private KafkaConsumer<String, FlowDocument> createConsumer() {
        final Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID().toString());
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaInputFlowDeserializer.class);
        final KafkaConsumer<String, FlowDocument> kafkaConsumer = new KafkaConsumer<>(consumerProps);
        kafkaConsumer.subscribe(Collections.singletonList(NephronOptions.DEFAULT_FLOW_SOURCE_TOPIC));
        return kafkaConsumer;
    }
}
