package revshop.order_service.order.dto;

public class CheckoutRequest {

    private String paymentMethod;

    public CheckoutRequest() {
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
