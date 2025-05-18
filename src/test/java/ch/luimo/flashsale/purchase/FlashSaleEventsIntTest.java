package ch.luimo.flashsale.purchase;

import ch.luimo.flashsale.eventservice.avro.AvroEventStatus;
import ch.luimo.flashsale.eventservice.avro.AvroFlashSaleEvent;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class FlashSaleEventsIntTest extends IntegrationTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(FlashSaleEventsIntTest.class);

    @Test
    public void testPublishFlashSaleEvent() {
        AvroFlashSaleEvent avroFlashSaleEvent = flashSaleEventOf();

        testProducer.publishEvent(avroFlashSaleEvent);

        LOG.info("Test event published: {}", avroFlashSaleEvent);
        assertEventPublished("");
    }

    private void assertEventPublished(String expectedEventId) {
        LOG.info("Starting await for event with ID: {}", expectedEventId);
        try {
            Awaitility.await()
                    .atMost(5, TimeUnit.SECONDS)
                    .with().pollInterval(Duration.ofMillis(500))
                    .untilAsserted(() -> {
                        LOG.info("Polling ...");
//                        ConsumerRecords<String, String> records = testProducer.poll(Duration.ofMillis(500));
//                        for (var record : records) {
//                            LOG.info("Successfully received record: key = {}, value = {}", record.key(), record.value());
//                            assertThat(record.key()).isEqualTo(expectedEventId);
//                        }
                        assertTrue(false);
                    });
        } finally {
            LOG.info("Closing Kafka consumer");
        }
        LOG.info("Await finished for event: {}", expectedEventId);
    }

    private AvroFlashSaleEvent flashSaleEventOf() {
        return AvroFlashSaleEvent.newBuilder()
                .setId(1L)
                .setEventName("test event")
                .setStartTime(Instant.now())
                .setDuration(3600)
                .setProductId(UUID.randomUUID().toString())
                .setSellerId(UUID.randomUUID().toString())
                .setStockQuantity(1000)
                .setMaxPerCustomer(10)
                .setEventStatus(AvroEventStatus.CREATED)
                .build();
    }
}
