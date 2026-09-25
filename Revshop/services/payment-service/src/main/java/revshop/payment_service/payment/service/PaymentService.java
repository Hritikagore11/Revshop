package revshop.payment_service.payment.service;

import org.springframework.stereotype.Service;
import revshop.payment_service.payment.model.Payment;
import revshop.payment_service.payment.repository.PaymentRepository;

import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public Payment createPayment(
            Long orderId,
            String paymentMethod,
            Double amount) {

        if (paymentRepository.findByOrderId(orderId).isPresent()) {
            throw new RuntimeException("Payment already exists for this order");
        }

        Payment payment = new Payment();

        payment.setOrderId(orderId);
        payment.setPaymentMethod(paymentMethod);
        payment.setAmount(amount);
        payment.setStatus("SUCCESS");
        payment.setTransactionId(UUID.randomUUID().toString());

        return paymentRepository.save(payment);
    }

    public Payment getPayment(Long id) {

        return paymentRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Payment not found"));
    }
}