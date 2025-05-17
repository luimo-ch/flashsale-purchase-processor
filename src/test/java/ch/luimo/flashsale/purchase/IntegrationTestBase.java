package ch.luimo.flashsale.purchase;

import ch.luimo.flashsale.purchase.config.FlashSaleEventsTestProducer;
import ch.luimo.flashsale.purchase.config.KafkaTestConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@SpringBootTest
@Import(KafkaTestConfig.class)
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    public static final String BOOTSTRAP_SERVERS_PROPERTY = "spring.kafka.bootstrap-servers";
    public static final String SCHEMA_REGISTRY_PROPERTY = "spring.kafka.properties.schema.registry.url";

    private static final Logger LOG = LoggerFactory.getLogger(IntegrationTestBase.class);

    protected static final PostgreSQLContainer<?> mysqlContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15.4"))
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

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

    @Value("${application.kafka-topics.flashsale-events}")
    protected String kafkaTopic;

    @Autowired
    protected FlashSaleEventsTestProducer testConsumer;

    @BeforeAll
    static void startContainers() {
        mysqlContainer.start();
        KAFKA_CONTAINER.start();
        SCHEMA_REGISTRY.start();

        System.setProperty("spring.datasource.url", mysqlContainer.getJdbcUrl());
        System.setProperty("spring.datasource.username", mysqlContainer.getUsername());
        System.setProperty("spring.datasource.password", mysqlContainer.getPassword());

        System.setProperty(BOOTSTRAP_SERVERS_PROPERTY, KAFKA_CONTAINER.getBootstrapServers());
        System.setProperty(SCHEMA_REGISTRY_PROPERTY, "http://localhost:" + SCHEMA_REGISTRY.getFirstMappedPort());

        LOG.info("Starting Kafka container at {}", KAFKA_CONTAINER.getBootstrapServers());
        LOG.info("Starting MySQL container at {}", mysqlContainer.getJdbcUrl());
    }

    @AfterAll
    static void stopKafka() {
        if (SCHEMA_REGISTRY != null) {
            SCHEMA_REGISTRY.stop();
        }
        if (KAFKA_CONTAINER != null) {
            KAFKA_CONTAINER.stop();
        }
    }
}
