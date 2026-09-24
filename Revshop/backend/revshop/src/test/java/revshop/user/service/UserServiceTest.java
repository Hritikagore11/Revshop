package revshop.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import revshop.exception.DuplicateEmailException;
import revshop.exception.InvalidCredentialException;
import revshop.exception.UserNotFoundException;
import revshop.security.JwtService;
import revshop.user.model.LoginResponse;
import revshop.user.model.User;
import revshop.user.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UserService userService;

    @Test
    void register_shouldSaveUserSuccessfully() {

        User user = new User();
        user.setName("Test User");
        user.setEmail("test@gmail.com");
        user.setPassword("password123");
        user.setRole("BUYER");

        when(userRepo.findByEmail(user.getEmail()))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode("password123"))
                .thenReturn("encodedPassword");

        when(userRepo.save(user))
                .thenReturn(user);

        User result = userService.register(user);

        assertEquals("BUYER", result.getRole());
        assertEquals("encodedPassword", result.getPassword());

        verify(userRepo).findByEmail("test@gmail.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepo).save(user);
    }

    @Test
    void register_shouldThrowExceptionForDuplicateEmail() {

        User user = new User();
        user.setEmail("existing@gmail.com");
        user.setPassword("password123");
        user.setRole("BUYER");

        when(userRepo.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        assertThrows(
                DuplicateEmailException.class,
                () -> userService.register(user)
        );

        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    void register_shouldRejectInvalidRole() {

        User user = new User();
        user.setEmail("test@gmail.com");
        user.setPassword("password123");
        user.setRole("ADMIN");

        when(userRepo.findByEmail(user.getEmail()))
                .thenReturn(Optional.empty());

        assertThrows(
                RuntimeException.class,
                () -> userService.register(user)
        );

        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    void login_shouldReturnJwtSuccessfully() {

        User user = new User();
        user.setId(1L);
        user.setEmail("test@gmail.com");
        user.setPassword("encodedPassword");
        user.setRole("BUYER");

        when(userRepo.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("password123", "encodedPassword"))
                .thenReturn(true);

        when(jwtService.generateToken(user))
                .thenReturn("test-jwt-token");

        LoginResponse response =
                userService.login("test@gmail.com", "password123");

        assertEquals("Login successful", response.getMessage());
        assertEquals("test-jwt-token", response.getToken());
        assertEquals("BUYER", response.getRole());
        assertEquals(1L, response.getUserId());

        verify(jwtService).generateToken(user);
    }

    @Test
    void login_shouldRejectWrongPassword() {

        User user = new User();
        user.setEmail("test@gmail.com");
        user.setPassword("encodedPassword");

        when(userRepo.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("wrongPassword", "encodedPassword"))
                .thenReturn(false);

        assertThrows(
                InvalidCredentialException.class,
                () -> userService.login(
                        "test@gmail.com",
                        "wrongPassword"
                )
        );

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void updateUser_shouldThrowExceptionWhenUserDoesNotExist() {

        when(userRepo.findById(99L))
                .thenReturn(Optional.empty());

        User updatedUser = new User();

        assertThrows(
                UserNotFoundException.class,
                () -> userService.updateUser(99L, updatedUser)
        );

        verify(userRepo, never()).save(any(User.class));
    }
}