package revshop.order_service.order.dto;

public class PaymentResponse {

    private Long id;
    private Long orderId;
    private String paymentMethod;
    private Double amount;
    private String status;
    private String transactionId;

    public PaymentResponse() {
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public Double getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
}