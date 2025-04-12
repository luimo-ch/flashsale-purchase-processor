package ch.luimo.flashsale.purchase.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;


@RestController("/purchases")
@Tag(name = "Purchase Requests", description = "Endpoints for submitting purchase requests")
public class PurchaseRequests {

    @GetMapping("/hello")
    public String getMethodName() {
        return "Hello World!";
    }
    

}
