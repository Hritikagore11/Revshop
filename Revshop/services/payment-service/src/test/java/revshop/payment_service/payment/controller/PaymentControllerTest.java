package revshop.payment_service.payment.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import revshop.payment_service.payment.model.Payment;
import revshop.payment_service.payment.service.PaymentService;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    private MockMvc mockMvc;

    private Payment payment;

    @BeforeEach
    void setUp() {

        PaymentController controller =
                new PaymentController(paymentService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();

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
    void createPayment_shouldReturnOk() throws Exception {

        when(paymentService.createPayment(
                10L,
                "CARD",
                5000.0
        )).thenReturn(payment);

        mockMvc.perform(post("/payments")
                        .param("orderId", "10")
                        .param("paymentMethod", "CARD")
                        .param("amount", "5000.0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderId").value(10))
                .andExpect(jsonPath("$.paymentMethod").value("CARD"))
                .andExpect(jsonPath("$.amount").value(5000.0))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(paymentService).createPayment(
                10L,
                "CARD",
                5000.0
        );
    }

    @Test
    void getPayment_shouldReturnOk() throws Exception {

        when(paymentService.getPayment(1L))
                .thenReturn(payment);

        mockMvc.perform(get("/payments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderId").value(10))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(paymentService).getPayment(1L);
    }

    @Test
    void getPaymentByOrderId_shouldReturnOk() throws Exception {

        when(paymentService.getPaymentByOrderId(10L))
                .thenReturn(payment);

        mockMvc.perform(get("/payments/order/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(10))
                .andExpect(jsonPath("$.paymentMethod").value("CARD"));

        verify(paymentService).getPaymentByOrderId(10L);
    }

    @Test
    void getPaymentStatus_shouldReturnOk() throws Exception {

        when(paymentService.getPaymentStatus(10L))
                .thenReturn("SUCCESS");

        mockMvc.perform(get("/payments/order/10/status"))
                .andExpect(status().isOk())
                .andExpect(content().string("SUCCESS"));

        verify(paymentService).getPaymentStatus(10L);
    }

    @Test
    void updatePaymentStatus_shouldReturnOk() throws Exception {

        payment.setStatus("FAILED");

        when(paymentService.updatePaymentStatus(
                10L,
                "FAILED"
        )).thenReturn(payment);

        mockMvc.perform(put("/payments/order/10/status")
                        .param("status", "FAILED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(10))
                .andExpect(jsonPath("$.status").value("FAILED"));

        verify(paymentService).updatePaymentStatus(
                10L,
                "FAILED"
        );
    }
}