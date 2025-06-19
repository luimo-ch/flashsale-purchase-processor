package ch.luimo.flashsale.purchase;

import ch.luimo.flashsale.eventservice.avro.AvroEventStatus;
import ch.luimo.flashsale.eventservice.avro.AvroFlashSaleEvent;
import ch.luimo.flashsale.purchase.service.FlashSaleEventCacheService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FlashSaleEventsIntTest extends IntegrationTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(FlashSaleEventsIntTest.class);

    @Autowired
    FlashSaleEventCacheService cacheService;

    @Test
    public void testPublishFlashSaleEvent_startAndEndEvent_shouldAddAndRemoveFromCache() {
        AvroFlashSaleEvent avroFlashSaleEvent = flashSaleEventOf();

        flashSaleEventsTestProducer.publishEvent(avroFlashSaleEvent);
        assertFlashSaleEventReceivedAndActivated(avroFlashSaleEvent.getEventId());

        avroFlashSaleEvent.setEventStatus(AvroEventStatus.ENDED);
        flashSaleEventsTestProducer.publishEvent(avroFlashSaleEvent);
        assertFlashSaleEventReceivedAndDeactivated(avroFlashSaleEvent.getEventId());
    }

    private void assertFlashSaleEventReceivedAndActivated(String expectedEventId) {
        pollUntilAsserted(() -> {
            LOG.info("Checking cache for STARTED event with ID: {}", expectedEventId);
            cacheService.printEvent(expectedEventId);
            assertTrue(cacheService.isEventActive(expectedEventId));
        });
    }

    private void assertFlashSaleEventReceivedAndDeactivated(String expectedEventId) {
        pollUntilAsserted(() -> {
            LOG.info("Checking cache for ENDED event with ID: {}", expectedEventId);
            cacheService.printEvent(expectedEventId);
            assertFalse(cacheService.isEventActive(expectedEventId));
        });
    }
}
