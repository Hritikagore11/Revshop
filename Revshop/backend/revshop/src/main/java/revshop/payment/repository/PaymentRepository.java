package revshop.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import revshop.payment.model.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}