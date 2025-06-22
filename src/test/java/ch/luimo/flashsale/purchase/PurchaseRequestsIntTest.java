package ch.luimo.flashsale.purchase;

import ch.luimo.flashsale.eventservice.avro.AvroFlashSaleEvent;
import ch.luimo.flashsale.purchase.service.FlashSaleEventCacheService;
import ch.luimode.flashsale.AvroPurchaseRequest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.UUID;

import static ch.luimo.flashsale.purchase.domain.PurchaseRequestStatus.*;
import static ch.luimo.flashsale.purchase.service.FlashSaleEventCacheService.PURCHASE_CACHE_KEY_PREFIX;
import static ch.luimo.flashsale.purchase.service.FlashSaleEventCacheService.PURCHASE_REQUEST_STATUS;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PurchaseRequestsIntTest extends IntegrationTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(FlashSaleEventsIntTest.class);

    @Autowired
    FlashSaleEventCacheService cacheService;

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    @Test
    public void testProcessPurchaseRequest_requestPending_setsConfirmationStatus() {
        // create a flash sale event with initial stock 1
        int initialQuantity = 1;
        AvroFlashSaleEvent avroFlashSaleEvent = flashSaleEventOf();
        avroFlashSaleEvent.setStockQuantity(initialQuantity);
        flashSaleEventsTestProducer.publishEvent(avroFlashSaleEvent);
        assertFlashSaleEventReceivedAndActivated(avroFlashSaleEvent.getEventId());
        assertThat(cacheService.getStock(avroFlashSaleEvent.getEventId())).isEqualTo(initialQuantity);

        // create purchase request for quantity 1
        AvroPurchaseRequest purchaseRequest = purchaseRequestOf(avroFlashSaleEvent.getEventId(), 1);
        createCachedPendingPurchaseRequest(purchaseRequest.getPurchaseId());

        purchaseRequestsTestProducer.publishEvent(purchaseRequest);
        assertPurchaseRequestConfirmed(purchaseRequest.getPurchaseId());

        // assert stock decreased
        int stockAfterPurchase = cacheService.getStock(avroFlashSaleEvent.getEventId());
        assertThat(stockAfterPurchase).isEqualTo(initialQuantity - purchaseRequest.getQuantity());
    }

    @Test
    public void testProcessPurchaseRequest_eventNotActive_rejectsPurchaseRequest() {
        AvroPurchaseRequest purchaseRequest = purchaseRequestOf(UUID.randomUUID().toString(), 1);
        createCachedPendingPurchaseRequest(purchaseRequest.getPurchaseId());

        purchaseRequestsTestProducer.publishEvent(purchaseRequest);
        assertPurchaseRequestRejected(purchaseRequest.getPurchaseId());
    }

    @Test
    public void testProcessPurchaseRequest_requestExceedsPerCustomerLimit_rejectsPurchaseRequest() {
        int initialQuantity = 100;
        int maxPerCustomerLimit = 5;
        AvroFlashSaleEvent avroFlashSaleEvent = flashSaleEventOf();
        avroFlashSaleEvent.setStockQuantity(initialQuantity);
        avroFlashSaleEvent.setMaxPerCustomer(maxPerCustomerLimit);
        flashSaleEventsTestProducer.publishEvent(avroFlashSaleEvent);
        assertFlashSaleEventReceivedAndActivated(avroFlashSaleEvent.getEventId());
        assertThat(cacheService.getStock(avroFlashSaleEvent.getEventId())).isEqualTo(initialQuantity);

        // create purchase request exceeding quantity
        AvroPurchaseRequest purchaseRequest = purchaseRequestOf(avroFlashSaleEvent.getEventId(), maxPerCustomerLimit + 1);
        createCachedPendingPurchaseRequest(purchaseRequest.getPurchaseId());

        purchaseRequestsTestProducer.publishEvent(purchaseRequest);
        assertPurchaseRequestRejected(purchaseRequest.getPurchaseId());

        // assert stock DID NOT decrease
        int stockAfterPurchase = cacheService.getStock(avroFlashSaleEvent.getEventId());
        assertThat(stockAfterPurchase).isEqualTo(initialQuantity);
    }

    @Test
    public void testProcessPurchaseRequest_outOfStock_rejectsPurchaseRequest() {
        int initialQuantity = 5;
        int requestedAmount = 6;
        int maxPerCustomerLimit = 10;
        AvroFlashSaleEvent avroFlashSaleEvent = flashSaleEventOf();
        avroFlashSaleEvent.setStockQuantity(initialQuantity);
        avroFlashSaleEvent.setMaxPerCustomer(maxPerCustomerLimit);
        flashSaleEventsTestProducer.publishEvent(avroFlashSaleEvent);
        assertFlashSaleEventReceivedAndActivated(avroFlashSaleEvent.getEventId());
        assertThat(cacheService.getStock(avroFlashSaleEvent.getEventId())).isEqualTo(initialQuantity);

        // create purchase request exceeding quantity
        AvroPurchaseRequest purchaseRequest = purchaseRequestOf(avroFlashSaleEvent.getEventId(), requestedAmount);
        createCachedPendingPurchaseRequest(purchaseRequest.getPurchaseId());

        purchaseRequestsTestProducer.publishEvent(purchaseRequest);
        assertPurchaseRequestRejected(purchaseRequest.getPurchaseId());

        // assert stock DID NOT decrease - the purchase request can be either place in full, or not at all
        int stockAfterPurchase = cacheService.getStock(avroFlashSaleEvent.getEventId());
        assertThat(stockAfterPurchase).isEqualTo(initialQuantity);
    }

    private void createCachedPendingPurchaseRequest(String purchaseRequestId) {
        HashOperations<String, String, String> opsForHash = redisTemplate.opsForHash();
        String key = PURCHASE_CACHE_KEY_PREFIX + purchaseRequestId;
        opsForHash.put(key, PURCHASE_REQUEST_STATUS, PENDING.name());
        String status = opsForHash.get(key, PURCHASE_REQUEST_STATUS);
        LOG.info("Cache:: Created purchase request with key {} and status {}", key, status);
    }

    private void assertFlashSaleEventReceivedAndActivated(String expectedEventId) {
        pollUntilAsserted(() -> {
            LOG.info("Checking cache for STARTED event with ID: {}", expectedEventId);
            cacheService.printEvent(expectedEventId);
            assertTrue(cacheService.isEventActive(expectedEventId));
        });
    }

    private void assertPurchaseRequestRejected(String purchaseRequestId) {
        pollUntilAsserted(() -> {
            LOG.info("Checking cache for REJECTED purchase request with ID: {}", purchaseRequestId);
            HashOperations<String, String, String> opsForHash = redisTemplate.opsForHash();
            String key = PURCHASE_CACHE_KEY_PREFIX + purchaseRequestId;
            String requestStatus = opsForHash.get(key, PURCHASE_REQUEST_STATUS);
            assertThat(requestStatus).isEqualTo(REJECTED.name());
        });
    }

    private void assertPurchaseRequestConfirmed(String purchaseRequestId) {
        pollUntilAsserted(() -> {
            LOG.info("Checking cache for CONFIRMED purchase request with ID: {}", purchaseRequestId);
            HashOperations<String, String, String> opsForHash = redisTemplate.opsForHash();
            String key = PURCHASE_CACHE_KEY_PREFIX + purchaseRequestId;
            String requestStatus = opsForHash.get(key, PURCHASE_REQUEST_STATUS);
            assertThat(requestStatus).isEqualTo(CONFIRMED.name());
        });
    }
}
