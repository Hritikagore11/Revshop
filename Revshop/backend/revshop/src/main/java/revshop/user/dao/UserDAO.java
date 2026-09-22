package revshop.user.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import revshop.user.model.User;

public interface UserDAO extends JpaRepository<User, Long> {
}
