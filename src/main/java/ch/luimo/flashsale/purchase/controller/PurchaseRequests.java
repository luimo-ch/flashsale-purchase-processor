package ch.luimo.flashsale.purchase.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController("/purchases")
@Tag(name = "Purchase Requests", description = "Endpoints for submitting purchase requests")
public class PurchaseRequests {

    @PostMapping()
    public String processPurchaseRequest() {
        return "Hello World!";
    }
    

}
