package ch.luimo.flashsale.purchase.consumer;

import ch.luimode.flashsale.PurchaseRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PurchaseRequestsProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(PurchaseRequestsProcessor.class);

    @KafkaListener(
            topics = "${application.kafka-topics.purchase-requests}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void processPurchaseRequests(PurchaseRequest purchaseRequest) {
        LOG.info("Received Message {} ", purchaseRequest);
    }

}
