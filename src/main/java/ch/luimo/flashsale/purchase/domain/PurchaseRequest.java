package ch.luimo.flashsale.purchase.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

@Getter
@Setter
@ToString
public class PurchaseRequest {
    private String eventId;
    private String purchaseId;
    private String userId;
    private String itemId;
    private int quantity;
    private Instant requestedAt;
    private SourceType source;

    public PurchaseRequest(String purchaseId, String userId, String itemId, int quantity, Instant requestedAt, SourceType source) {
        this.purchaseId = purchaseId;
        this.userId = userId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.requestedAt = requestedAt;
        this.source = source;
    }

    public PurchaseRequest() {
    }

    public enum SourceType {
        WEB, MOBILE, API
    }
}