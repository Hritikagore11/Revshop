package revshop.payment_service.payment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import revshop.payment_service.payment.model.Payment;
import revshop.payment_service.payment.repository.PaymentRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Payment payment;

    @BeforeEach
    void setUp() {
        payment = new Payment(
                1L,
                10L,
                "CARD",
                5000.0,
                "SUCCESS",
                "txn-123"
        );
    }

    @Test
    void createPayment_shouldCreateSuccessfully() {

        when(paymentRepository.findByOrderId(10L))
                .thenReturn(Optional.empty());

        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(payment);

        Payment result = paymentService.createPayment(
                10L,
                "CARD",
                5000.0
        );

        assertNotNull(result);
        assertEquals(10L, result.getOrderId());
        assertEquals("CARD", result.getPaymentMethod());
        assertEquals(5000.0, result.getAmount());
        assertEquals("SUCCESS", result.getStatus());
        assertNotNull(result.getTransactionId());

        verify(paymentRepository).findByOrderId(10L);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void createPayment_shouldRejectDuplicatePayment() {

        when(paymentRepository.findByOrderId(10L))
                .thenReturn(Optional.of(payment));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> paymentService.createPayment(
                        10L,
                        "CARD",
                        5000.0
                )
        );

        assertEquals(
                "Payment already exists for this order",
                exception.getMessage()
        );

        verify(paymentRepository).findByOrderId(10L);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void getPayment_shouldReturnPayment() {

        when(paymentRepository.findById(1L))
                .thenReturn(Optional.of(payment));

        Payment result = paymentService.getPayment(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(10L, result.getOrderId());
        assertEquals("CARD", result.getPaymentMethod());

        verify(paymentRepository).findById(1L);
    }

    @Test
    void getPayment_shouldThrowExceptionWhenNotFound() {

        when(paymentRepository.findById(99L))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> paymentService.getPayment(99L)
        );

        assertEquals(
                "Payment not found",
                exception.getMessage()
        );

        verify(paymentRepository).findById(99L);
    }

    @Test
    void getPaymentByOrderId_shouldReturnPayment() {

        when(paymentRepository.findByOrderId(10L))
                .thenReturn(Optional.of(payment));

        Payment result =
                paymentService.getPaymentByOrderId(10L);

        assertNotNull(result);
        assertEquals(10L, result.getOrderId());
        assertEquals("SUCCESS", result.getStatus());

        verify(paymentRepository).findByOrderId(10L);
    }

    @Test
    void updatePaymentStatus_shouldUpdateSuccessfully() {

        when(paymentRepository.findByOrderId(10L))
                .thenReturn(Optional.of(payment));

        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(payment);

        Payment result =
                paymentService.updatePaymentStatus(
                        10L,
                        "FAILED"
                );

        assertNotNull(result);
        assertEquals("FAILED", result.getStatus());

        verify(paymentRepository).findByOrderId(10L);
        verify(paymentRepository).save(payment);
    }
}