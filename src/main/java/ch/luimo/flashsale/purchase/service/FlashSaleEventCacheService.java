package ch.luimo.flashsale.purchase.service;

import ch.luimo.flashsale.eventservice.avro.AvroFlashSaleEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.stereotype.Service;

@Service
public class FlashSaleEventCacheService {

    private static final Logger LOG = LoggerFactory.getLogger(FlashSaleEventCacheService.class);

    private static final String KEY_EVENT_NAME = "eventName";
    private static final String KEY_START_TIME = "startTime";
    private static final String KEY_DURATION = "duration";
    private static final String KEY_PRODUCTID = "productId";
    private static final String KEY_SELLER_ID = "sellerId";
    private static final String KEY_STOCK_QUANTITY = "stockQuantity";
    private static final String KEY_MAX_PER_CUSTOMER = "maxPerCustomer";
    private static final String KEY_EVENT_STATUS = "eventStatus";

    private static final String EVENT_HASH_PREFIX = "flashsale:event:";
    private static final String ACTIVE_EVENTS_SET = "flashsale:active";

    private final RedisTemplate<String, String> redisTemplate;
    private final HashOperations<String, String, String> hashOps;
    private final SetOperations<String, String> setOps;

    public FlashSaleEventCacheService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.hashOps = redisTemplate.opsForHash();
        this.setOps = redisTemplate.opsForSet();
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

        // set holding the currently active flashsale events
        setOps.add(ACTIVE_EVENTS_SET, String.valueOf(event.getId()));
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
        if(deleted){
            setOps.remove(ACTIVE_EVENTS_SET, String.valueOf(eventId));
        }else {
            LOG.warn("Event with id {} not found!", eventId);
        }
    }

    public boolean isEventActive(long eventId) {
        return Boolean.TRUE.equals(setOps.isMember(ACTIVE_EVENTS_SET, String.valueOf(eventId)));
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
