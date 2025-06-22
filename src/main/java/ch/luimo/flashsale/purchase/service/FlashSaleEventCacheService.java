package ch.luimo.flashsale.purchase.service;

import ch.luimo.flashsale.eventservice.avro.AvroFlashSaleEvent;
import ch.luimo.flashsale.purchase.domain.PurchaseRequestStatus;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class FlashSaleEventCacheService {

    private static final Logger LOG = LoggerFactory.getLogger(FlashSaleEventCacheService.class);

    private static final String EVENT_HASH_KEY_PREFIX = "flashsale-purchase-processor:event:";

    private static final String KEY_EVENT_NAME = "eventName";
    private static final String KEY_START_TIME = "startTime";
    private static final String KEY_DURATION = "duration";
    private static final String KEY_PRODUCTID = "productId";
    private static final String KEY_SELLER_ID = "sellerId";
    private static final String KEY_STOCK_QUANTITY = "stockQuantity";
    private static final String KEY_MAX_PER_CUSTOMER = "maxPerCustomer";
    private static final String KEY_EVENT_STATUS = "eventStatus";

    // this is a shared redis key!
    public static final String PURCHASE_CACHE_KEY_PREFIX = "flashsale:purchase:";
    public static final String PURCHASE_REQUEST_STATUS = "status";
    public static final String PURCHASE_REQUEST_REJECTION_REASON = "reason";

    private final RedisTemplate<String, String> redisTemplate;
    private final HashOperations<String, String, String> hashOps;

    public FlashSaleEventCacheService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.hashOps = redisTemplate.opsForHash();
    }

    public void addEvent(AvroFlashSaleEvent event) {
        String key = EVENT_HASH_KEY_PREFIX + event.getEventId();

        hashOps.put(key, KEY_EVENT_NAME, event.getEventName());
        hashOps.put(key, KEY_START_TIME, String.valueOf(event.getStartTime()));
        hashOps.put(key, KEY_DURATION, String.valueOf(event.getDuration()));
        hashOps.put(key, KEY_PRODUCTID, event.getProductId());
        hashOps.put(key, KEY_SELLER_ID, event.getSellerId());
        hashOps.put(key, KEY_STOCK_QUANTITY, String.valueOf(event.getStockQuantity()));
        hashOps.put(key, KEY_MAX_PER_CUSTOMER, String.valueOf(event.getMaxPerCustomer()));
        hashOps.put(key, KEY_EVENT_STATUS, event.getEventStatus().name());
    }

    public void printEvent(String id) {
        String key = EVENT_HASH_KEY_PREFIX + id;
        String eventName = hashOps.get(key, KEY_EVENT_NAME);
        String stockQuantity = hashOps.get(key, KEY_STOCK_QUANTITY);
        String eventStatus = hashOps.get(key, KEY_EVENT_STATUS);
        String maxPerCustomer = hashOps.get(key, KEY_MAX_PER_CUSTOMER);
        LOG.info("Event {}, quantity {}, status {}, maxPerCustomer:{}", eventName, stockQuantity, eventStatus, maxPerCustomer);
    }

    public void removeEvent(String eventId) {
        String key = EVENT_HASH_KEY_PREFIX + eventId;
        Boolean deleted = redisTemplate.delete(key);
        if(deleted) {
            LOG.info("Event with key {} was deleted ", key);
        } else  {
            LOG.warn("Unable to delete event with key {}. It does not exist!", key);
        }
    }

    public boolean isEventActive(String eventId) {
        String key = EVENT_HASH_KEY_PREFIX + eventId;
        String eventStatus = hashOps.get(key, KEY_EVENT_STATUS);
        return StringUtils.isNotBlank(eventStatus);
    }

    public int getPerCustomerPurchaseLimit(String eventId) {
        String key = EVENT_HASH_KEY_PREFIX + eventId;
        String maxPerCustomerStr = hashOps.get(key, KEY_MAX_PER_CUSTOMER);
        if(StringUtils.isBlank(maxPerCustomerStr)){
            throw new IllegalArgumentException("Max per customer limit is empty");
        }
        return Integer.parseInt(maxPerCustomerStr);
    }

    public void decrementStock(String eventId, int amount) {
        String key = EVENT_HASH_KEY_PREFIX + eventId;
        hashOps.increment(key, KEY_STOCK_QUANTITY, (amount * -1));
    }

    public int getStock(String eventId) {
        String key = EVENT_HASH_KEY_PREFIX + eventId;
        String stock = hashOps.get(key, KEY_STOCK_QUANTITY);
        return stock != null ? Integer.parseInt(stock) : 0;
    }

    public void setRequestRejection(String eventId, String purchaseId, String reason) {
        String purchaseRequestKey = PURCHASE_CACHE_KEY_PREFIX + purchaseId;
        String currentStatus = hashOps.get(purchaseRequestKey, PURCHASE_REQUEST_STATUS);
        if (StringUtils.isBlank(currentStatus)) {
            LOG.error("Purchase request status with key {} was not found! Skipping rejection.", purchaseRequestKey);
        }
        if (!PurchaseRequestStatus.PENDING.name().equalsIgnoreCase(currentStatus)) {
            LOG.warn("Purchase request {}  for event {} was not in status pending! It was {}", purchaseId,  eventId, currentStatus);
        }
        hashOps.put(purchaseRequestKey, PURCHASE_REQUEST_STATUS, PurchaseRequestStatus.REJECTED.name());
        hashOps.put(purchaseRequestKey, PURCHASE_REQUEST_REJECTION_REASON, reason);
    }

    public void setRequestConfirmation(String purchaseId) {
        String purchaseRequestKey = PURCHASE_CACHE_KEY_PREFIX + purchaseId;
        String currentStatus = hashOps.get(purchaseRequestKey, PURCHASE_REQUEST_STATUS);
        if (StringUtils.isBlank(currentStatus)) {
            LOG.error("Purchase request status with key {} was not found! Skipping confirmation.", purchaseRequestKey);
        }
        if (!PurchaseRequestStatus.isPending(currentStatus)) {
            LOG.error("Purchase request status with key {} was not pending! It is {}. Skipping confirmation.", purchaseRequestKey,  currentStatus);
        }
        hashOps.put(purchaseRequestKey, PURCHASE_REQUEST_STATUS, PurchaseRequestStatus.CONFIRMED.name());
    }
}
