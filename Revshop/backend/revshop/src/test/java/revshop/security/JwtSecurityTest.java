package revshop.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import revshop.user.model.User;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    @Test
    void generateToken_shouldCreateToken() {

        User user = new User();
        user.setId(1L);
        user.setEmail("test@gmail.com");
        user.setRole("BUYER");

        String token = jwtService.generateToken(user);

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void extractEmail_shouldReturnUserEmail() {

        User user = new User();
        user.setId(1L);
        user.setEmail("test@gmail.com");
        user.setRole("BUYER");

        String token = jwtService.generateToken(user);

        String email = jwtService.extractEmail(token);

        assertEquals("test@gmail.com", email);
    }

    @Test
    void extractRole_shouldReturnUserRole() {

        User user = new User();
        user.setId(1L);
        user.setEmail("test@gmail.com");
        user.setRole("BUYER");

        String token = jwtService.generateToken(user);

        String role = jwtService.extractRole(token);

        assertEquals("BUYER", role);
    }

    @Test
    void extractUserId_shouldReturnUserId() {

        User user = new User();
        user.setId(1L);
        user.setEmail("test@gmail.com");
        user.setRole("BUYER");

        String token = jwtService.generateToken(user);

        Long userId = jwtService.extractUserId(token);

        assertEquals(1L, userId);
    }

    @Test
    void isTokenValid_shouldReturnTrueForValidUser() {

        User user = new User();
        user.setId(1L);
        user.setEmail("test@gmail.com");
        user.setRole("BUYER");

        String token = jwtService.generateToken(user);

        assertTrue(jwtService.isTokenValid(token, user));
    }
}