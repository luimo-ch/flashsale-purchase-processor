package ch.luimo.flashsale.purchase.config;

import ch.luimo.flashsale.eventservice.avro.AvroFlashSaleEvent;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Properties;

import static ch.luimo.flashsale.purchase.IntegrationTestBase.BOOTSTRAP_SERVERS_PROPERTY;
import static ch.luimo.flashsale.purchase.IntegrationTestBase.SCHEMA_REGISTRY_PROPERTY;

@TestConfiguration
public class KafkaTestConfig {

    @Autowired
    private KafkaTemplate<String, AvroFlashSaleEvent> kafkaTemplate;

    @Bean
    public FlashSaleEventsTestProducer flashSaleEventsTestProducer() {
//        String bootstrapServers = System.getProperty(BOOTSTRAP_SERVERS_PROPERTY);
//        Properties props = new Properties();
//        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
//        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
//        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
//        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
//        props.put("schema.registry.url", System.getProperty(SCHEMA_REGISTRY_PROPERTY));
//        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
//
//        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        return new FlashSaleEventsTestProducer(kafkaTemplate);
    }
}