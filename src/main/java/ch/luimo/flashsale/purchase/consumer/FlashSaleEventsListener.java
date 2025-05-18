package ch.luimo.flashsale.purchase.consumer;

import ch.luimo.flashsale.eventservice.avro.AvroFlashSaleEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class FlashSaleEventsListener {
    private static final Logger LOG = LoggerFactory.getLogger(FlashSaleEventsListener.class);

    @KafkaListener(
            topics = "${application.kafka-topics.flashsale-events}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void processFlashsaleEvents(AvroFlashSaleEvent avroFlashSaleEvent) {
        LOG.info("FlashSaleEventsListener Received Message {} ", avroFlashSaleEvent);
    }
}
