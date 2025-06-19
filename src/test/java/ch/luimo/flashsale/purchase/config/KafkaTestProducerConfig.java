package ch.luimo.flashsale.purchase.config;

import ch.luimo.flashsale.eventservice.avro.AvroFlashSaleEvent;
import ch.luimode.flashsale.AvroPurchaseRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

@TestConfiguration
public class KafkaTestProducerConfig {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaTestProducerConfig.class);

    @Autowired
    private KafkaTemplate<String, AvroFlashSaleEvent> flashSaleEventKafkaTemplate;

    @Autowired
    KafkaTemplate<String, AvroPurchaseRequest> purchaseRequestKafkaTemplate;

    @Bean
    public FlashSaleEventsTestProducer flashSaleEventsTestProducer() {
        return new FlashSaleEventsTestProducer(flashSaleEventKafkaTemplate);
    }

    @Bean
    public PurchaseRequestsTestProducer purchaseRequestsTestProducer() {
        return new PurchaseRequestsTestProducer(purchaseRequestKafkaTemplate);
    }

    public static class FlashSaleEventsTestProducer {

        @Value("${application.kafka-topics.flashsale-events}")
        private String flashSaleEventsTopic;

        private final KafkaTemplate<String, AvroFlashSaleEvent> kafkaTemplate;

        public FlashSaleEventsTestProducer(KafkaTemplate<String, AvroFlashSaleEvent> kafkaTemplate) {
            this.kafkaTemplate = kafkaTemplate;
        }

        public void publishEvent(AvroFlashSaleEvent event) {
            LOG.info("Publishing test event to topic {}: {}", flashSaleEventsTopic, event.getEventId());
            kafkaTemplate.send(flashSaleEventsTopic, String.valueOf(event.getEventId()), event)
                    .thenRun(() -> LOG.info("Publishing flash sale event finished: {}", event))
                    .exceptionally(ex -> {
                        LOG.error("Error publishing flash sale event", ex);
                        return null;
                    });
        }
    }

    public static class PurchaseRequestsTestProducer {

        @Value("${application.kafka-topics.purchase-requests}")
        private String purchaseRequestsTopic;

        private final KafkaTemplate<String, AvroPurchaseRequest> kafkaTemplate;

        public PurchaseRequestsTestProducer(KafkaTemplate<String, AvroPurchaseRequest> kafkaTemplate) {
            this.kafkaTemplate = kafkaTemplate;
        }

        public void publishEvent(AvroPurchaseRequest purchaseRequest) {
            LOG.info("Publishing purchase request to topic {}: purchaseReqId {}", purchaseRequestsTopic, purchaseRequest.getEventId());
            kafkaTemplate.send(purchaseRequestsTopic, String.valueOf(purchaseRequest.getEventId()), purchaseRequest)
                    .thenRun(() -> LOG.info("Publishing purchase request finished: {}", purchaseRequest))
                    .exceptionally(ex -> {
                        LOG.error("Error publishing purchase request", ex);
                        return null;
                    });
        }
    }
}