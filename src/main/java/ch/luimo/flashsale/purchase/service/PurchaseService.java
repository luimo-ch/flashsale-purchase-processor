package ch.luimo.flashsale.purchase.service;

import ch.luimo.flashsale.purchase.domain.PurchaseRequest;
import ch.luimode.flashsale.AvroPurchaseRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class PurchaseService {

    private static final Logger LOG = LoggerFactory.getLogger(PurchaseService.class);

    private final FlashSaleEventCacheService flashSaleEventCacheService;

    public PurchaseService(KafkaTemplate<String, AvroPurchaseRequest> kafkaTemplate,
                           FlashSaleEventCacheService flashSaleEventCacheService) {
        this.flashSaleEventCacheService = flashSaleEventCacheService;
    }

    public void processPurchaseRequest(PurchaseRequest purchaseRequest) {
        validateRequest(purchaseRequest);
        flashSaleEventCacheService.decrementStock(purchaseRequest.getEventId(), purchaseRequest.getQuantity());
        flashSaleEventCacheService.setRequestConfirmation(purchaseRequest.getPurchaseId());
    }

    private void validateRequest(PurchaseRequest purchaseRequest) {
        String eventId = purchaseRequest.getEventId();
        if (!flashSaleEventCacheService.isEventActive(eventId)) {
            rejectPurchaseRequest(purchaseRequest, "Event " + eventId + " is not yet active!");
        }
        int perCustomerPurchaseLimit = flashSaleEventCacheService.getPerCustomerPurchaseLimit(eventId);
        if (purchaseRequest.getQuantity() >  perCustomerPurchaseLimit) {
            rejectPurchaseRequest(purchaseRequest, "Event " + eventId + " has a limit of {} per person!");
        }
        int stock = flashSaleEventCacheService.getStock(eventId);
        if (stock < purchaseRequest.getQuantity()) {
            rejectPurchaseRequest(purchaseRequest, "Event " + eventId + " has not enough stock!");
        }
    }

    private void rejectPurchaseRequest(PurchaseRequest purchaseRequest, String reason) {
        flashSaleEventCacheService.setRequestRejection(purchaseRequest.getEventId(),
                purchaseRequest.getPurchaseId(), reason);
    }
}
