package revshop.user_service.user.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email already registered"
            );
        }

        if (user.getRole() == null ||
                (!user.getRole().equalsIgnoreCase("BUYER") &&
                        !user.getRole().equalsIgnoreCase("SELLER"))) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Role must be BUYER or SELLER"
            );
        }

        if (user.getPassword() == null ||
                user.getPassword().length() < 6) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password must be at least 6 characters"
            );
        }

        user.setRole(user.getRole().toUpperCase());

        user.setPassword(
                passwordEncoder.encode(user.getPassword())
        );

        return userRepo.save(user);
    }

    public String login(
            String email,
            String password) {

        User user = userRepo.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Invalid email or password"
                        ));

        if (!passwordEncoder.matches(
                password,
                user.getPassword())) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password"
            );
        }

        return jwtService.generateToken(user);
    }

    public User updateUser(
            Long id,
            User updatedUser) {

        User existingUser =
                userRepo.findById(id)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "User not found"
                                ));

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
                        new RuntimeException("User not found"));
    }

    public void deleteUser(Long id) {

        if (!userRepo.existsById(id)) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "User not found"
            );
        }

        userRepo.deleteById(id);
    }

    public User getProfile(
            Authentication authentication) {

        User user =
                (User) authentication.getPrincipal();

        return userRepo.findById(user.getId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        ));
    }

    public User updateProfile(
            Authentication authentication,
            User updatedUser) {

        User existingUser =
                (User) authentication.getPrincipal();

        existingUser.setName(updatedUser.getName());
        existingUser.setPhone(updatedUser.getPhone());
        existingUser.setAddress(updatedUser.getAddress());

        return userRepo.save(existingUser);
    }
}