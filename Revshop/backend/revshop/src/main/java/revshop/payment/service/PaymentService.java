package revshop.payment.service;

import org.springframework.stereotype.Service;
import revshop.order.model.Order;
import revshop.order.repository.OrderRepository;
import revshop.payment.exception.InvalidPaymentException;
import revshop.payment.exception.OrderNotFoundException;
import revshop.payment.exception.PaymentNotFoundException;
import revshop.payment.model.Payment;
import revshop.payment.repository.PaymentRepository;

import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    public PaymentService(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository) {

        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
    }

    public Payment createPayment(
            Long orderId,
            String paymentMethod) {

        if (orderId == null) {
            throw new InvalidPaymentException(
                    "Order ID is required"
            );
        }

        if (paymentMethod == null ||
                paymentMethod.trim().isEmpty()) {

            throw new InvalidPaymentException(
                    "Payment method is required"
            );
        }

        String method = paymentMethod.trim().toUpperCase();

        if (!method.equals("COD") &&
                !method.equals("CARD")) {

            throw new InvalidPaymentException(
                    "Invalid payment method. Use COD or CARD"
            );
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new OrderNotFoundException(
                                "Order not found with id: " + orderId
                        )
                );

        if (paymentRepository.findByOrderId(orderId).isPresent()) {
            throw new InvalidPaymentException(
                    "Payment already exists for this order"
            );
        }

        Payment payment = new Payment();

        payment.setOrder(order);
        payment.setPaymentMethod(method);
        payment.setAmount(order.getTotalAmount());

        // Payment simulation
        payment.setStatus("SUCCESS");

        payment.setTransactionId(
                UUID.randomUUID().toString()
        );

        return paymentRepository.save(payment);
    }

    public Payment getPayment(Long id) {

        if (id == null) {
            throw new InvalidPaymentException(
                    "Payment ID is required"
            );
        }

        return paymentRepository.findById(id)
                .orElseThrow(() ->
                        new PaymentNotFoundException(
                                "Payment not found with id: " + id
                        )
                );
    }
}