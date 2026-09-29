package revshop;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import revshop.payment.controller.PaymentController;
import revshop.payment.model.Payment;
import revshop.payment.service.PaymentService;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
public class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;


    // POST /payments - COD
    @Test
    void createPayment_WithCOD_ShouldReturn200() throws Exception {

        Payment payment = new Payment();

        payment.setId(1L);
        payment.setPaymentMethod("COD");
        payment.setAmount(3296.0);
        payment.setStatus("SUCCESS");
        payment.setTransactionId("TXN-12345");

        when(paymentService.createPayment(1L, "COD"))
                .thenReturn(payment);

        mockMvc.perform(
                        post("/payments")
                                .param("orderId", "1")
                                .param("paymentMethod", "COD")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.paymentMethod").value("COD"))
                .andExpect(jsonPath("$.amount").value(3296.0))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(
                        jsonPath("$.transactionId")
                                .value("TXN-12345")
                );
    }


    // POST /payments - CARD
    @Test
    void createPayment_WithCard_ShouldReturn200()
            throws Exception {

        Payment payment = new Payment();

        payment.setId(2L);
        payment.setPaymentMethod("CARD");
        payment.setAmount(3296.0);
        payment.setStatus("SUCCESS");
        payment.setTransactionId("TXN-67890");

        when(paymentService.createPayment(1L, "CARD"))
                .thenReturn(payment);

        mockMvc.perform(
                        post("/payments")
                                .param("orderId", "1")
                                .param("paymentMethod", "CARD")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.paymentMethod")
                                .value("CARD")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("SUCCESS")
                );
    }


    // GET /payments/{id}
    @Test
    void getPayment_WhenPaymentExists_ShouldReturn200()
            throws Exception {

        Payment payment = new Payment();

        payment.setId(1L);
        payment.setPaymentMethod("COD");
        payment.setAmount(3296.0);
        payment.setStatus("SUCCESS");
        payment.setTransactionId("TXN-12345");

        when(paymentService.getPayment(1L))
                .thenReturn(payment);

        mockMvc.perform(
                        get("/payments/1")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(
                        jsonPath("$.paymentMethod")
                                .value("COD")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("SUCCESS")
                );
    }
}