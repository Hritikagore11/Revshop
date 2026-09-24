package revshop.user.service;

import org.springframework.stereotype.Service;
import revshop.exception.DuplicateEmailException;
import revshop.exception.UserNotFoundException;
import revshop.user.repository.UserRepository;
import revshop.user.model.User;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepo;

    public UserService(UserRepository userDAO) {
        this.userRepo = userDAO;
    }

    public User register(User user){
        if(userRepo.findByEmail(user.getEmail()).isPresent()){
            throw new DuplicateEmailException("Email already registered");
        }

        if(user.getRole() == null || (!user.getRole().equalsIgnoreCase("BUYER") && !user.getRole().equalsIgnoreCase("SELLER"))){
            throw new RuntimeException("Role must be BUYER or SELLER");
        }
        user.setRole(user.getRole().toUpperCase());
        return userRepo.save(user);
    }

    public User login(String email, String password){
        User user = userRepo.findByEmail(email).orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if(!user.getPassword().equals(password)){
            throw new DuplicateEmailException("Invalid email or password");
        }
        return user;
    }
    public User updateUser(Long id, User updatedUser){
        User existingUser = userRepo.findById(id).orElseThrow(() -> new UserNotFoundException("User not found"));
        existingUser.setName(updatedUser.getName());
        existingUser.setPhone(updatedUser.getPhone());
        existingUser.setAddress(updatedUser.getAddress());
        return userRepo.save(existingUser);
    }

    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    public User getUserById(Long id) {
        return userRepo.findById(id).orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    public void deleteUser(Long id) {
        userRepo.deleteById(id);
    }
}