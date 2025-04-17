package ch.luimo.flashsale.purchase.config;

import ch.luimode.flashsale.PurchaseRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${application.kafka-api-key}")
    private String kafkaApiKey;

    @Value("${application.kafka-api-secret}")
    private String kafkaApiSecret;

    @Value("${application.schema-api-key}")
    private String schemaApiKey;

    @Value("${application.schema-api-secret}")
    private String schemaApiSecret;

    @Bean
    public String kafkaJaasConfig() {
        System.out.println("Kafka JAAS Config: " + kafkaApiKey + " " + kafkaApiSecret);
        return String.format(
                "org.apache.kafka.common.security.plain.PlainLoginModule required username='%s' password='%s';",
                kafkaApiKey,
                kafkaApiSecret
        );
    }

    @Bean
    public Map<String, Object> kafkaProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put("sasl.jaas.config", kafkaJaasConfig());
        props.put("security.protocol", "SASL_SSL");
        props.put("sasl.mechanism", "PLAIN");
        props.put("basic.auth.credentials.source", "USER_INFO");
        props.put("basic.auth.user.info", schemaRegistryCredentials());
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "io.confluent.kafka.serializers.KafkaAvroSerializer");
        props.put("schema.registry.url", "https://psrc-9mwvv.europe-west6.gcp.confluent.cloud");
        props.put("bootstrap.servers", "pkc-lzoyy.europe-west6.gcp.confluent.cloud:9092");
        return props;
    }

    @Bean
    public String schemaRegistryCredentials() {
        return schemaApiKey + ":" + schemaApiSecret;
    }

    @Bean
    public ProducerFactory<String, PurchaseRequest> producerFactory() {
        Map<String, Object> configProps = kafkaProperties();
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, PurchaseRequest> kafkaTemplate(ProducerFactory<String, PurchaseRequest> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }


}
