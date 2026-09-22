package revshop.user.service;

import org.springframework.stereotype.Service;
import revshop.user.dao.UserDAO;
import revshop.user.model.User;

import java.util.List;

@Service
public class UserService {

    private final UserDAO userDAO;

    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public User addUser(User user) {
        return userDAO.save(user);
    }

    public List<User> getAllUsers() {
        return userDAO.findAll();
    }

    public User getUserById(Long id) {
        return userDAO.findById(id).orElse(null);
    }

    public void deleteUser(Long id) {
        userDAO.deleteById(id);
    }
}