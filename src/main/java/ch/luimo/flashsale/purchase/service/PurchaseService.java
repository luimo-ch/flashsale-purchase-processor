package ch.luimo.flashsale.purchase.service;

import ch.luimode.flashsale.PurchaseRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class PurchaseService {

    private static final Logger LOG = LoggerFactory.getLogger(PurchaseService.class);

    private final KafkaTemplate<String, PurchaseRequest> kafkaTemplate;

    @Value("${application.kafka-topics.purchase-requests}")
    private String purchaseRequestsTopic;

    public PurchaseService(KafkaTemplate<String, PurchaseRequest> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(String topic, PurchaseRequest avroMessage) {
        kafkaTemplate.send(topic, "key", avroMessage);
    }
}
