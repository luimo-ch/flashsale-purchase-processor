package ch.luimo.flashsale.purchase;

import ch.luimo.flashsale.eventservice.avro.AvroEventStatus;
import ch.luimo.flashsale.eventservice.avro.AvroFlashSaleEvent;
import ch.luimo.flashsale.purchase.config.FlashSaleEventsTestProducer;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

public class FlashSaleEventsIntTest extends IntegrationTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(FlashSaleEventsIntTest.class);

    @Autowired
    private FlashSaleEventsTestProducer flashSaleEventsTestProducer;

    @Test
    public void testPublishFlashSaleEvent() {
        AvroFlashSaleEvent avroFlashSaleEvent = flashSaleEventOf();

        flashSaleEventsTestProducer.publishEvent(avroFlashSaleEvent);

        LOG.info("Test event published: {}", avroFlashSaleEvent);
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
