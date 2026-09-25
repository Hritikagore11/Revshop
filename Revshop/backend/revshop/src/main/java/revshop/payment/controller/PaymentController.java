package revshop.payment.controller;

import org.springframework.web.bind.annotation.*;
import revshop.payment.model.Payment;
import revshop.payment.service.PaymentService;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public Payment createPayment(
            @RequestParam Long orderId,
            @RequestParam String paymentMethod) {

        return paymentService.createPayment(orderId, paymentMethod);
    }

    @GetMapping("/{id}")
    public Payment getPayment(@PathVariable Long id) {
        return paymentService.getPayment(id);
    }
}