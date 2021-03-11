![](catheter.png)

Utility for testing OpenNMS nephron flow processing. This tool generates a stream of pseudo-random flow records that add up to a given byte rate.

For a given point in time a flow ends with a probability that depends the given values for `minFlowDuration` and `maxFlowDuration`. Flow records will be created for these flows and published to a Kafka topic. Byte rates of these flows that ended at this point in time are summed up. Then a random number of flows depending on the value for `maxFlowCount` are spawned and the summed byte rate is distributed to these new flows. 

             > < Point in time
    |---------| Byte rate flow A
    ----||------- ongoing flow
    ----------| Byte rate flow B
    ------------- ongoing flow
    --||------| Byte rate flow C
    ----||------- ongoing flow
        
So, the byte rate can be combined of more or less flows. The following example spawns two flows for the three ended before.

             > < Point in time
    ----||------- ongoing flow
    ------------- ongoing flow
    ----||------- ongoing flow
              |-- Byte rate (A + B + C) / 2
              |-- Byte rate (A + B + C) / 2

This will result in a pretty random stream of flows that total up to the requested byte rate. The following example code will create a realtime simulation with three exporters. The third exporter has an skewed clock with a five minutes offset:


    final Simulation simulation = Simulation.builder(handler)
                .withTickMs(Duration.ofMillis(100))
                .withRealtime(true)
                .withExporters(Exporter.builder()
                        .withNodeId(1)
                        .withForeignSource("Test")
                        .withForeignId("Router1")
                        .withLocation("Fulda")
                        .withBytesPerSecond(1_100_000L)
                        .withActiveTimeout(Duration.ofSeconds(1))
                        .withMaxFlowCount(100)
                        .withInputSnmp(12)
                        .withOutputSnmp(21))
                .withExporters(Exporter.builder()
                        .withNodeId(2)
                        .withForeignSource("Test")
                        .withForeignId("Router2")
                        .withLocation("Ottawa")
                        .withBytesPerSecond(1_200_000L)
                        .withActiveTimeout(Duration.ofSeconds(1))
                        .withMaxFlowCount(100)
                        .withInputSnmp(13)
                        .withOutputSnmp(31))
                .withExporters(Exporter.builder()
                        .withClockOffset(Duration.ofMinutes(5))
                        .withNodeId(3)
                        .withForeignSource("Test")
                        .withForeignId("Router3")
                        .withLocation("Raleigh")
                        .withBytesPerSecond(1_300_000L)
                        .withActiveTimeout(Duration.ofSeconds(1))
                        .withMaxFlowCount(100)
                        .withInputSnmp(14)
                        .withOutputSnmp(41))
                .withStartTime(start)
                .build();

    simulation.start();
    Thread.sleep(10_000L);
    simulation.stop();
    simulation.join();
    
A realtime simulation sleeps for the given `tickMs` for each iteration. A non-realtime simulation will run as fast as possible.
    

