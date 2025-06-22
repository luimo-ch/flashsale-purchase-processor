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
        ValidationResult validationResult = validateRequest(purchaseRequest);
        if (!validationResult.isValid()) {
            LOG.warn("Validation failed for {} with reason: {}", purchaseRequest.getPurchaseId(), validationResult.reason());
            rejectPurchaseRequest(purchaseRequest, validationResult.reason());
        } else {
            flashSaleEventCacheService.decrementStock(purchaseRequest.getEventId(), purchaseRequest.getQuantity());
            flashSaleEventCacheService.setRequestConfirmation(purchaseRequest.getPurchaseId());
        }
    }

    private ValidationResult validateRequest(PurchaseRequest purchaseRequest) {
        String eventId = purchaseRequest.getEventId();
        if (!flashSaleEventCacheService.isEventActive(eventId)) {
            return new ValidationResult(false, "Event " + eventId + " is not yet active!");

        }
        int perCustomerPurchaseLimit = flashSaleEventCacheService.getPerCustomerPurchaseLimit(eventId);
        if (purchaseRequest.getQuantity() >  perCustomerPurchaseLimit) {
            return new ValidationResult(false,
                    "Event " + eventId + " has a limit of " + perCustomerPurchaseLimit +" per person! Requested: " + purchaseRequest.getQuantity());
        }
        int stock = flashSaleEventCacheService.getStock(eventId);
        if (stock < purchaseRequest.getQuantity()) {
            return new ValidationResult(false, "Event " + eventId + " has not enough stock!");
        }
        return new ValidationResult(true, "");
    }

    private void rejectPurchaseRequest(PurchaseRequest purchaseRequest, String reason) {
        flashSaleEventCacheService.setRequestRejection(purchaseRequest.getEventId(),
                purchaseRequest.getPurchaseId(), reason);
    }
}
