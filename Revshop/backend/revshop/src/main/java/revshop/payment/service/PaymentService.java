package revshop.payment.service;

import org.springframework.stereotype.Service;
import revshop.order.model.Order;
import revshop.order.repository.OrderRepository;
import revshop.payment.model.Payment;
import revshop.payment.repository.PaymentRepository;

import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    public PaymentService(PaymentRepository paymentRepository,
                          OrderRepository orderRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
    }


    public Payment createPayment(Long orderId, String paymentMethod) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Order not found"));

        if (paymentMethod == null ||
                paymentMethod.trim().isEmpty()) {

            throw new RuntimeException(
                    "Payment method is required");
        }

        if (!paymentMethod.equalsIgnoreCase("CARD") &&
                !paymentMethod.equalsIgnoreCase("COD")) {

            throw new RuntimeException(
                    "Invalid payment method. Use CARD or COD");
        }


        if (paymentRepository.findByOrderId(orderId).isPresent()) {

            throw new RuntimeException(
                    "Payment already exists for this order");
        }

        Payment payment = new Payment();

        payment.setOrder(order);
        payment.setPaymentMethod(
                paymentMethod.toUpperCase());
        payment.setAmount(order.getTotalAmount());

        if (paymentMethod.equalsIgnoreCase("COD")) {

            payment.setStatus("PENDING");

        } else {


            payment.setStatus("SUCCESS");
        }

        payment.setTransactionId(
                UUID.randomUUID().toString());

        return paymentRepository.save(payment);
    }


    public Payment getPayment(Long id) {

        return paymentRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Payment not found"));
    }


    public List<Payment> getAllPayments() {

        return paymentRepository.findAll();
    }


    public Payment getPaymentByOrderId(Long orderId) {

        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Payment not found for order: "
                                        + orderId));
    }


    public String getPaymentStatus(Long id) {

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Payment not found"));

        return payment.getStatus();
    }


    public Payment updatePaymentStatus(
            Long id,
            String status) {

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Payment not found"));

        if (status == null ||
                status.trim().isEmpty()) {

            throw new RuntimeException(
                    "Payment status is required");
        }

        if (!status.equalsIgnoreCase("SUCCESS") &&
                !status.equalsIgnoreCase("PENDING") &&
                !status.equalsIgnoreCase("FAILED")) {

            throw new RuntimeException(
                    "Invalid payment status");
        }

        payment.setStatus(status.toUpperCase());

        return paymentRepository.save(payment);
    }
}