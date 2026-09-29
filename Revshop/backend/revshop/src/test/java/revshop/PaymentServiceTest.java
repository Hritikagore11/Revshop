package revshop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import revshop.order.model.Order;
import revshop.order.repository.OrderRepository;
import revshop.payment.exception.InvalidPaymentException;
import revshop.payment.exception.OrderNotFoundException;
import revshop.payment.exception.PaymentNotFoundException;
import revshop.payment.model.Payment;
import revshop.payment.repository.PaymentRepository;
import revshop.payment.service.PaymentService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Order order;

    @BeforeEach
    void setUp() {

        order = new Order();

        order.setId(1L);
        order.setTotalAmount(3296.0);
    }

    @Test
    void createPayment_WithValidCOD_ShouldCreatePayment() {

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(paymentRepository.findByOrderId(1L))
                .thenReturn(Optional.empty());

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Payment result =
                paymentService.createPayment(1L, "COD");

        assertNotNull(result);
        assertEquals("COD", result.getPaymentMethod());
        assertEquals(3296.0, result.getAmount());
        assertEquals("SUCCESS", result.getStatus());
        assertNotNull(result.getTransactionId());

        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void createPayment_WithValidCard_ShouldCreatePayment() {

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(paymentRepository.findByOrderId(1L))
                .thenReturn(Optional.empty());

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Payment result =
                paymentService.createPayment(1L, "CARD");

        assertNotNull(result);
        assertEquals("CARD", result.getPaymentMethod());
        assertEquals("SUCCESS", result.getStatus());
        assertNotNull(result.getTransactionId());
    }

    @Test
    void createPayment_WithInvalidPaymentMethod_ShouldThrowException() {

        assertThrows(
                InvalidPaymentException.class,
                () -> paymentService.createPayment(
                        1L,
                        "UPI"
                )
        );
    }

    @Test
    void createPayment_WithNullPaymentMethod_ShouldThrowException() {

        assertThrows(
                InvalidPaymentException.class,
                () -> paymentService.createPayment(
                        1L,
                        null
                )
        );
    }

    @Test
    void createPayment_WhenOrderDoesNotExist_ShouldThrowException() {

        when(orderRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                OrderNotFoundException.class,
                () -> paymentService.createPayment(
                        99L,
                        "COD"
                )
        );

        verify(paymentRepository, never())
                .save(any(Payment.class));
    }

    @Test
    void createPayment_WhenPaymentAlreadyExists_ShouldThrowException() {

        Payment existingPayment = new Payment();

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(paymentRepository.findByOrderId(1L))
                .thenReturn(Optional.of(existingPayment));

        assertThrows(
                InvalidPaymentException.class,
                () -> paymentService.createPayment(
                        1L,
                        "COD"
                )
        );
    }

    @Test
    void getPayment_WhenPaymentExists_ShouldReturnPayment() {

        Payment payment = new Payment();

        payment.setId(1L);
        payment.setPaymentMethod("COD");
        payment.setAmount(3296.0);
        payment.setStatus("SUCCESS");

        when(paymentRepository.findById(1L))
                .thenReturn(Optional.of(payment));

        Payment result =
                paymentService.getPayment(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("COD", result.getPaymentMethod());
        assertEquals("SUCCESS", result.getStatus());
    }

    @Test
    void getPayment_WhenPaymentDoesNotExist_ShouldThrowException() {

        when(paymentRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                PaymentNotFoundException.class,
                () -> paymentService.getPayment(99L)
        );
    }

    @Test
    void createPayment_WithNullOrderId_ShouldThrowException() {

        assertThrows(
                InvalidPaymentException.class,
                () -> paymentService.createPayment(
                        null,
                        "COD"
                )
        );
    }
}