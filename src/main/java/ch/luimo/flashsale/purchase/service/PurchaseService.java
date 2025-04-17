package ch.luimo.flashsale.purchase.service;

import ch.luimode.flashsale.PurchaseRequest;
import ch.luimode.flashsale.SourceType;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

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

    @PostConstruct
    public void sendFirstMessage() {
        sendMessage(purchaseRequestsTopic, createPurchaseMsg(1, "item-123", "user-123"));
        LOG.info("Sent msg to topic {} ", purchaseRequestsTopic);
    }

    public PurchaseRequest createPurchaseMsg(int quantity, String itemId, String userId) {
        return PurchaseRequest.newBuilder()
                .setPurchaseId(UUID.randomUUID().toString())
                .setRequestedAt(Instant.now())
                .setQuantity(quantity)
                .setSource(SourceType.WEB)
                .setItemId(itemId)
                .setUserId(userId)
                .build();
    }
}
