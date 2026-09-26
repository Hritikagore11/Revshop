package revshop.user_service.user.service;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import revshop.user_service.security.JwtService;
import revshop.user_service.user.model.User;
import revshop.user_service.user.repository.UserRepository;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(
            UserRepository userRepo,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public User register(User user) {

        if (userRepo.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        if (user.getRole() == null ||
                (!user.getRole().equalsIgnoreCase("BUYER") &&
                        !user.getRole().equalsIgnoreCase("SELLER"))) {

            throw new RuntimeException("Role must be BUYER or SELLER");
        }

        if (user.getPassword() == null || user.getPassword().length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters");
        }

        user.setRole(user.getRole().toUpperCase());

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepo.save(user);
    }

    public String login(String email, String password) {

        User user = userRepo.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        return jwtService.generateToken(user);
    }

    public User updateUser(Long id, User updatedUser) {

        User existingUser = userRepo.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        existingUser.setName(updatedUser.getName());
        existingUser.setPhone(updatedUser.getPhone());
        existingUser.setAddress(updatedUser.getAddress());

        return userRepo.save(existingUser);
    }

    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    public User getUserById(Long id) {
        return userRepo.findById(id).orElse(null);
    }

    public void deleteUser(Long id) {
        userRepo.deleteById(id);
    }
    public User getProfile(Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        return userRepo.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    public User updateProfile(
            Authentication authentication,
            User updatedUser) {

        User existingUser = (User) authentication.getPrincipal();

        existingUser.setName(updatedUser.getName());
        existingUser.setPhone(updatedUser.getPhone());
        existingUser.setAddress(updatedUser.getAddress());

        return userRepo.save(existingUser);
    }
}