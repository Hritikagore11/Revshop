package revshop.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import revshop.exception.DuplicateEmailException;
import revshop.exception.InvalidCredentialException;
import revshop.exception.UserNotFoundException;
import revshop.security.JwtService;
import revshop.user.model.LoginResponse;
import revshop.user.model.User;
import revshop.user.repository.UserRepository;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserRepository userRepo,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public User register(User user) {

        if (userRepo.findByEmail(user.getEmail()).isPresent()) {
            throw new DuplicateEmailException("Email already registered");
        }

        if (user.getRole() == null ||
                (!user.getRole().equalsIgnoreCase("BUYER") &&
                        !user.getRole().equalsIgnoreCase("SELLER"))) {

            throw new RuntimeException("Role must be BUYER or SELLER");
        }

        user.setRole(user.getRole().toUpperCase());

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepo.save(user);
    }

    public LoginResponse login(String email, String password) {

        User user = userRepo.findByEmail(email)
                .orElseThrow(() ->
                        new InvalidCredentialException(
                                "Invalid email or password"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialException(
                    "Invalid email or password");
        }

        String token = jwtService.generateToken(user);

        return new LoginResponse("Login successful", token,user.getRole(), user.getId());
    }

    public User updateUser(Long id, User updatedUser) {

        User existingUser = userRepo.findById(id)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        existingUser.setName(updatedUser.getName());
        existingUser.setPhone(updatedUser.getPhone());
        existingUser.setAddress(updatedUser.getAddress());

        return userRepo.save(existingUser);
    }

    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    public User getUserById(Long id) {

        return userRepo.findById(id)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));
    }

    public void deleteUser(Long id) {
        userRepo.deleteById(id);
    }
}