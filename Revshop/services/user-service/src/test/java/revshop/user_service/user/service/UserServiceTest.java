package revshop.user_service.user.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import revshop.user_service.security.JwtService;
import revshop.user_service.user.model.User;
import revshop.user_service.user.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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


    // 1. Register user successfully
    @Test
    void register_shouldCreateUser() {

        User user = new User();
        user.setName("Test User");
        user.setEmail("test@gmail.com");
        user.setPassword("123456");
        user.setRole("BUYER");

        when(userRepo.findByEmail("test@gmail.com"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode("123456"))
                .thenReturn("encodedPassword");

        when(userRepo.save(any(User.class)))
                .thenReturn(user);

        User result = userService.register(user);

        assertNotNull(result);
        assertEquals("BUYER", result.getRole());
        assertEquals("encodedPassword", result.getPassword());

        verify(userRepo).findByEmail("test@gmail.com");
        verify(passwordEncoder).encode("123456");
        verify(userRepo).save(user);
    }


    // 2. Duplicate email should be rejected
    @Test
    void register_shouldRejectDuplicateEmail() {

        User user = new User();
        user.setEmail("test@gmail.com");
        user.setPassword("123456");
        user.setRole("BUYER");

        when(userRepo.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(user));

        assertThrows(
                RuntimeException.class,
                () -> userService.register(user)
        );

        verify(userRepo).findByEmail("test@gmail.com");
        verify(userRepo, never()).save(any(User.class));
    }


    // 3. Login successfully
    @Test
    void login_shouldReturnToken() {

        User user = new User();
        user.setId(1L);
        user.setEmail("test@gmail.com");
        user.setPassword("encodedPassword");
        user.setRole("BUYER");

        when(userRepo.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "123456",
                "encodedPassword"))
                .thenReturn(true);

        when(jwtService.generateToken(user))
                .thenReturn("test-jwt-token");

        String result =
                userService.login(
                        "test@gmail.com",
                        "123456"
                );

        assertEquals("test-jwt-token", result);

        verify(userRepo).findByEmail("test@gmail.com");
        verify(passwordEncoder)
                .matches("123456", "encodedPassword");
        verify(jwtService).generateToken(user);
    }


    // 4. Invalid password should be rejected
    @Test
    void login_shouldRejectInvalidPassword() {

        User user = new User();
        user.setEmail("test@gmail.com");
        user.setPassword("encodedPassword");

        when(userRepo.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "wrongPassword",
                "encodedPassword"))
                .thenReturn(false);

        assertThrows(
                RuntimeException.class,
                () -> userService.login(
                        "test@gmail.com",
                        "wrongPassword"
                )
        );

        verify(userRepo).findByEmail("test@gmail.com");
        verify(passwordEncoder)
                .matches("wrongPassword", "encodedPassword");

        verify(jwtService, never())
                .generateToken(any(User.class));
    }


    // 5. Update user successfully
    @Test
    void updateUser_shouldUpdateUser() {

        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setName("Old Name");
        existingUser.setPhone("1111111111");
        existingUser.setAddress("Pune");

        User updatedUser = new User();
        updatedUser.setName("New Name");
        updatedUser.setPhone("9999999999");
        updatedUser.setAddress("Mumbai");

        when(userRepo.findById(1L))
                .thenReturn(Optional.of(existingUser));

        when(userRepo.save(existingUser))
                .thenReturn(existingUser);

        User result =
                userService.updateUser(
                        1L,
                        updatedUser
                );

        assertEquals("New Name", result.getName());
        assertEquals("9999999999", result.getPhone());
        assertEquals("Mumbai", result.getAddress());

        verify(userRepo).findById(1L);
        verify(userRepo).save(existingUser);
    }


    // 6. Delete user successfully
    @Test
    void deleteUser_shouldDeleteUser() {

        when(userRepo.existsById(1L))
                .thenReturn(true);

        userService.deleteUser(1L);

        verify(userRepo).existsById(1L);
        verify(userRepo).deleteById(1L);
    }
}