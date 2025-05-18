package ch.luimo.flashsale.purchase;

import ch.luimo.flashsale.eventservice.avro.AvroEventStatus;
import ch.luimo.flashsale.eventservice.avro.AvroFlashSaleEvent;
import ch.luimo.flashsale.purchase.service.FlashSaleEventCacheService;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class FlashSaleEventsIntTest extends IntegrationTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(FlashSaleEventsIntTest.class);

    @Autowired
    FlashSaleEventCacheService cacheService;

    @Test
    public void testPublishFlashSaleEvent() {
        AvroFlashSaleEvent avroFlashSaleEvent = flashSaleEventOf();

        testProducer.publishEvent(avroFlashSaleEvent);

        LOG.info("Test event published: {}", avroFlashSaleEvent);
        assertEventCached(avroFlashSaleEvent.getId());
    }

    private void assertEventCached(Long expectedEventId) {
        LOG.info("Starting await for event with ID: {}", expectedEventId);
        try {
            Awaitility.await()
                    .atMost(5, TimeUnit.SECONDS)
                    .with().pollInterval(Duration.ofMillis(500))
                    .untilAsserted(() -> {
                        LOG.info("Checking cache for event with ID: {}", expectedEventId);
                        assertTrue(cacheService.isActive(expectedEventId));
                    });
        } finally {
            LOG.info("Ending await for event with ID: {}", expectedEventId);
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
                .setEventStatus(AvroEventStatus.STARTED)
                .build();
    }
}
