package revshop.payment_service.payment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import revshop.payment_service.payment.model.Payment;
import revshop.payment_service.payment.service.PaymentService;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<Payment> createPayment(
            @RequestParam Long orderId,
            @RequestParam String paymentMethod,
            @RequestParam Double amount) {

        return ResponseEntity.ok(
                paymentService.createPayment(
                        orderId,
                        paymentMethod,
                        amount
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPayment(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                paymentService.getPayment(id)
        );
    }
}