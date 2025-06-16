package ch.luimo.flashsale.purchase.service;

import ch.luimo.flashsale.eventservice.avro.AvroFlashSaleEvent;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class FlashSaleEventCacheService {

    private static final Logger LOG = LoggerFactory.getLogger(FlashSaleEventCacheService.class);

    private static final String EVENT_HASH_PREFIX = "flashsale-purchase-processor:event:";

    private static final String KEY_EVENT_NAME = "eventName";
    private static final String KEY_START_TIME = "startTime";
    private static final String KEY_DURATION = "duration";
    private static final String KEY_PRODUCTID = "productId";
    private static final String KEY_SELLER_ID = "sellerId";
    private static final String KEY_STOCK_QUANTITY = "stockQuantity";
    private static final String KEY_MAX_PER_CUSTOMER = "maxPerCustomer";
    private static final String KEY_EVENT_STATUS = "eventStatus";

    // this is a shared redis key!
    private static final String PURCHASE_CACHE_KEY_PREFIX = "flashsale:purchase:";
    private static final String PURCHASE_REQUEST_STATUS = "status";
    private static final String PURCHASE_REQUEST_REJECTION_REASON = "reason";

    private final RedisTemplate<String, String> redisTemplate;
    private final HashOperations<String, String, String> hashOps;

    public FlashSaleEventCacheService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.hashOps = redisTemplate.opsForHash();
    }

    public void addEvent(AvroFlashSaleEvent event) {
        String key = EVENT_HASH_PREFIX + event.getId();

        hashOps.put(key, KEY_EVENT_NAME, event.getEventName());
        hashOps.put(key, KEY_START_TIME, String.valueOf(event.getStartTime()));
        hashOps.put(key, KEY_DURATION, String.valueOf(event.getDuration()));
        hashOps.put(key, KEY_PRODUCTID, event.getProductId());
        hashOps.put(key, KEY_SELLER_ID, event.getSellerId());
        hashOps.put(key, KEY_STOCK_QUANTITY, String.valueOf(event.getStockQuantity()));
        hashOps.put(key, KEY_MAX_PER_CUSTOMER, String.valueOf(event.getMaxPerCustomer()));
        hashOps.put(key, KEY_EVENT_STATUS, event.getEventStatus().name());
    }

    public void printEvent(long id) {
        String key = EVENT_HASH_PREFIX + id;
        String eventName = hashOps.get(key, KEY_EVENT_NAME);
        String stockQuantity = hashOps.get(key, KEY_STOCK_QUANTITY);
        String eventStatus = hashOps.get(key, KEY_EVENT_STATUS);
        LOG.info(eventName + " " + stockQuantity + " " + eventStatus);
    }

    public void removeEvent(long eventId) {
        String key = EVENT_HASH_PREFIX + eventId;
        Boolean deleted = redisTemplate.delete(key);
        if(deleted) {
            LOG.info("Event with key {} was deleted ", key);
        } else  {
            LOG.warn("Unable to delete event with key {}. It does not exist!", key);
        }
    }

    public boolean isEventActive(long eventId) {
        String key = EVENT_HASH_PREFIX + eventId;
        String eventStatus = hashOps.get(key, KEY_EVENT_STATUS);
        return StringUtils.isNotBlank(eventStatus);
    }

    public int getPerCustomerPurchaseLimit(long eventId) {
        String key = EVENT_HASH_PREFIX + eventId;
        String maxPerCustomerStr = hashOps.get(key, KEY_MAX_PER_CUSTOMER);
        if(StringUtils.isBlank(maxPerCustomerStr)){
            throw new IllegalArgumentException("Max per customer limit is empty");
        }
        return Integer.parseInt(maxPerCustomerStr);
    }

    public void decrementStock(long eventId) {
        String key = EVENT_HASH_PREFIX + eventId;
        hashOps.increment(key, "stockQuantity", -1);
    }

    public int getStock(long eventId) {
        String key = EVENT_HASH_PREFIX + eventId;
        String stock = hashOps.get(key, "stockQuantity");
        return stock != null ? Integer.parseInt(stock) : 0;
    }
}
