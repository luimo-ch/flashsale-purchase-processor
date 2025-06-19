package ch.luimo.flashsale.purchase.consumer;

import ch.luimo.flashsale.purchase.domain.PurchaseRequest;
import ch.luimo.flashsale.purchase.service.PurchaseService;
import ch.luimode.flashsale.AvroPurchaseRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PurchaseRequestsProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(PurchaseRequestsProcessor.class);

    private final PurchaseService purchaseService;

    public PurchaseRequestsProcessor(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @KafkaListener(
            topics = "${application.kafka-topics.purchase-requests}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void processPurchaseRequests(AvroPurchaseRequest avroPurchaseRequest) {
        LOG.info("Received Message {} ", avroPurchaseRequest);
        purchaseService.processPurchaseRequest(toPurchaseRequest(avroPurchaseRequest));
    }


    private static PurchaseRequest toPurchaseRequest(AvroPurchaseRequest purchaseRequest) {
        PurchaseRequest p = new PurchaseRequest();
        p.setEventId(purchaseRequest.getEventId());
        p.setPurchaseId(purchaseRequest.getPurchaseId());
        p.setRequestedAt(purchaseRequest.getRequestedAt());
        p.setUserId(purchaseRequest.getUserId());
        p.setItemId(purchaseRequest.getItemId());
        p.setQuantity(purchaseRequest.getQuantity());
        p.setSource(toSourceType(purchaseRequest));
        return p;
    }

    private static PurchaseRequest.SourceType toSourceType(AvroPurchaseRequest purchaseRequest) {
        return switch (purchaseRequest.getSource()) {
            case WEB -> PurchaseRequest.SourceType.WEB;
            case MOBILE -> PurchaseRequest.SourceType.MOBILE;
            case API -> PurchaseRequest.SourceType.API;
        };
    }

}
