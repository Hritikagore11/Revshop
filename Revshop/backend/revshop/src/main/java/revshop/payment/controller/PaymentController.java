package revshop.payment.controller;

import org.springframework.web.bind.annotation.*;
import revshop.payment.model.Payment;
import revshop.payment.service.PaymentService;

import java.util.List;

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

        return paymentService.createPayment(
                orderId,
                paymentMethod
        );
    }


    @GetMapping("/{id}")
    public Payment getPayment(
            @PathVariable Long id) {

        return paymentService.getPayment(id);
    }


    @GetMapping
    public List<Payment> getAllPayments() {

        return paymentService.getAllPayments();
    }


    @GetMapping("/order/{orderId}")
    public Payment getPaymentByOrderId(
            @PathVariable Long orderId) {

        return paymentService.getPaymentByOrderId(
                orderId
        );
    }


    @GetMapping("/{id}/status")
    public String getPaymentStatus(
            @PathVariable Long id) {

        return paymentService.getPaymentStatus(id);
    }


    @PutMapping("/{id}/status")
    public Payment updatePaymentStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        return paymentService.updatePaymentStatus(
                id,
                status
        );
    }
}