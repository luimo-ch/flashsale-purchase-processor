package ch.luimo.flashsale.purchase.service;

import ch.luimo.flashsale.eventservice.avro.AvroFlashSaleEvent;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.stereotype.Service;

@Service
public class FlashSaleEventCacheService {

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

        hashOps.put(key, "eventName", event.getEventName());
        hashOps.put(key, "startTime", String.valueOf(event.getStartTime()));
        hashOps.put(key, "duration", String.valueOf(event.getDuration()));
        hashOps.put(key, "productId", event.getProductId());
        hashOps.put(key, "sellerId", event.getSellerId());
        hashOps.put(key, "stockQuantity", String.valueOf(event.getStockQuantity()));
        hashOps.put(key, "maxPerCustomer", String.valueOf(event.getMaxPerCustomer()));
        hashOps.put(key, "eventStatus", event.getEventStatus().name());

        // set holding the currently active flashsale events
        setOps.add(ACTIVE_EVENTS_SET, String.valueOf(event.getId()));
    }

    public void removeEvent(long eventId) {
        String key = EVENT_HASH_PREFIX + eventId;
        redisTemplate.delete(key);
        setOps.remove(ACTIVE_EVENTS_SET, String.valueOf(eventId));
    }

    public boolean isActive(long eventId) {
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
