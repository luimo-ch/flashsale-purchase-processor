package ch.luimo.flashsale.purchase;

import ch.luimo.flashsale.eventservice.avro.AvroEventStatus;
import ch.luimo.flashsale.eventservice.avro.AvroFlashSaleEvent;
import ch.luimo.flashsale.purchase.config.KafkaTestProducerConfig;
import ch.luimode.flashsale.AvroPurchaseRequest;
import ch.luimode.flashsale.SourceType;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.awaitility.Awaitility;
import org.awaitility.core.ThrowingRunnable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@Import(KafkaTestProducerConfig.class)
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    public static final String BOOTSTRAP_SERVERS_PROPERTY = "KAFKA_BOOTSTRAP_SERVERS";
    public static final String SCHEMA_REGISTRY_PROPERTY = "SCHEMA_REGISTRY_URL";

    private static final Logger LOG = LoggerFactory.getLogger(IntegrationTestBase.class);

    private static final GenericContainer<?> REDIS_CONTAINER = new GenericContainer<>("redis:7.0.12")
            .withExposedPorts(6379);

    public static final String CONFLUENT_PLATFORM_VERSION = "7.4.0";
    private static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("confluentinc/cp-kafka")
            .asCompatibleSubstituteFor("apache/kafka")
            .withTag(CONFLUENT_PLATFORM_VERSION);
    private static final DockerImageName SCHEMA_REGISTRY_IMAGE = DockerImageName.parse("confluentinc/cp-schema-registry")
            .withTag(CONFLUENT_PLATFORM_VERSION);
    private static final Network KAFKA_NETWORK = Network.newNetwork();

    private static final ConfluentKafkaContainer KAFKA_CONTAINER = new ConfluentKafkaContainer(KAFKA_IMAGE)
            .withListener("kafka:19092")
            .withNetwork(KAFKA_NETWORK)
            .withReuse(true);

    private static final GenericContainer<?> SCHEMA_REGISTRY = new GenericContainer<>(SCHEMA_REGISTRY_IMAGE)
            .withExposedPorts(8085)
            .withNetworkAliases("schemaregistry")
            .withNetwork(KAFKA_NETWORK)
            .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://kafka:19092")
            .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8085")
            .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schemaregistry")
            .withEnv("SCHEMA_REGISTRY_KAFKASTORE_SECURITY_PROTOCOL", "PLAINTEXT")
            .waitingFor(Wait.forHttp("/subjects"))
            .withStartupTimeout(Duration.of(120, ChronoUnit.SECONDS));

    @Autowired
    protected KafkaTestProducerConfig.FlashSaleEventsTestProducer flashSaleEventsTestProducer;

    @Autowired
    protected KafkaTestProducerConfig.PurchaseRequestsTestProducer purchaseRequestsTestProducer;

    @BeforeAll
    static void startContainers() {
        KAFKA_CONTAINER.start();
        SCHEMA_REGISTRY.start();
        REDIS_CONTAINER.start();

        System.setProperty("spring.data.redis.host", REDIS_CONTAINER.getHost());
        System.setProperty("spring.data.redis.port", REDIS_CONTAINER.getMappedPort(6379).toString());

        System.setProperty(BOOTSTRAP_SERVERS_PROPERTY, KAFKA_CONTAINER.getBootstrapServers());
        System.setProperty(SCHEMA_REGISTRY_PROPERTY, "http://localhost:" + SCHEMA_REGISTRY.getFirstMappedPort());

        LOG.info("Started Kafka container at {}", KAFKA_CONTAINER.getBootstrapServers());
        LOG.info("Started Redis container at {}", REDIS_CONTAINER.getHost() + ":" + REDIS_CONTAINER.getMappedPort(6379));

        createTestKafkaTopic("flashsale.purchase.requests", KAFKA_CONTAINER.getBootstrapServers(), false);
        createTestKafkaTopic("flashsale.events", KAFKA_CONTAINER.getBootstrapServers(), true);
    }

    private static void createTestKafkaTopic(String topicName, String bootstrapServers, boolean logCompaction) {
        try (AdminClient adminClient = AdminClient.create(Map.of("bootstrap.servers", bootstrapServers))) {
            NewTopic topic = new NewTopic(topicName, 1, (short) 1);
            if(logCompaction) {
                topic = topic.configs(Map.of("cleanup.policy", "compact"));
            }
            adminClient.createTopics(List.of(topic)).all().get();
            LOG.info("Created topic {}", topicName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create topic " + topicName, e);
        }
    }

    @AfterAll
    static void stopKafka() {
        if (SCHEMA_REGISTRY != null) {
            SCHEMA_REGISTRY.stop();
        }
        if (KAFKA_CONTAINER != null) {
            KAFKA_CONTAINER.stop();
        }
        if (REDIS_CONTAINER != null) {
            REDIS_CONTAINER.stop();
        }
    }

    protected void pollUntilAsserted(ThrowingRunnable runnable) {
        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .with().pollInterval(Duration.ofMillis(500))
                .untilAsserted(runnable);
    }

    protected AvroFlashSaleEvent flashSaleEventOf() {
        return AvroFlashSaleEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setEventName("test event")
                .setStartTime(Instant.now())
                .setDuration(3600)
                .setProductId(UUID.randomUUID().toString())
                .setSellerId(UUID.randomUUID().toString())
                .setStockQuantity(1000)
                .setMaxPerCustomer(10)
                .setEventStatus(AvroEventStatus.STARTED)
                .build();
    }

    protected AvroPurchaseRequest purchaseRequestOf(String eventId, int quantity) {
        return AvroPurchaseRequest.newBuilder()
                .setEventId(eventId)
                .setRequestedAt(Instant.now())
                .setQuantity(quantity)
                .setSource(SourceType.WEB)
                .setUserId(UUID.randomUUID().toString())
                .setItemId(UUID.randomUUID().toString())
                .setPurchaseId(UUID.randomUUID().toString())
                .build();
    }
}
