package ch.luimo.flashsale.purchase.consumer;

import ch.luimo.flashsale.eventservice.avro.AvroFlashSaleEvent;
import ch.luimo.flashsale.purchase.service.FlashSaleEventCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class FlashSaleEventsListener {
    private static final Logger LOG = LoggerFactory.getLogger(FlashSaleEventsListener.class);

    private final FlashSaleEventCacheService cacheService;

    public FlashSaleEventsListener(FlashSaleEventCacheService cacheService) {
        this.cacheService = cacheService;
    }

    @KafkaListener(
            topics = "${application.kafka-topics.flashsale-events}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void processFlashsaleEvents(AvroFlashSaleEvent avroFlashSaleEvent) {
        LOG.info("FlashSaleEventsListener Received Message {} ", avroFlashSaleEvent);

        switch (avroFlashSaleEvent.getEventStatus()) {
            case STARTED -> cacheService.addEvent(avroFlashSaleEvent);
            case ENDED, CANCELLED -> cacheService.removeEvent(avroFlashSaleEvent.getEventId());
            default -> LOG.info("No cache action for event status {}", avroFlashSaleEvent.getEventStatus());
        }
    }
}
