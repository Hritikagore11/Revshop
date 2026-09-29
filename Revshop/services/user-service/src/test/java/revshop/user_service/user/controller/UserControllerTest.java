package revshop.user_service.user.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import revshop.user_service.user.model.User;
import revshop.user_service.user.service.UserService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UserControllerTest {

    private UserService userService;
    private UserController userController;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        userController = new UserController(userService);
    }


    @Test
    void register_shouldReturnUser() {

        User user = new User();
        user.setName("Test User");
        user.setEmail("test@gmail.com");
        user.setRole("BUYER");

        when(userService.register(any(User.class)))
                .thenReturn(user);

        User result = userController.register(user);

        assertNotNull(result);
        assertEquals("Test User", result.getName());
        assertEquals("test@gmail.com", result.getEmail());

        verify(userService).register(user);
    }


    @Test
    void login_shouldReturnToken() {

        when(userService.login(
                "test@gmail.com",
                "password123"))
                .thenReturn("test-jwt-token");

        String result = userController.login(
                "test@gmail.com",
                "password123"
        );

        assertEquals("test-jwt-token", result);

        verify(userService).login(
                "test@gmail.com",
                "password123"
        );
    }


    @Test
    void getAllUsers_shouldReturnUsers() {

        User user = new User();
        user.setId(1L);
        user.setName("Test User");

        when(userService.getAllUsers())
                .thenReturn(List.of(user));

        List<User> result =
                userController.getAllUsers();

        assertEquals(1, result.size());
        assertEquals("Test User", result.get(0).getName());

        verify(userService).getAllUsers();
    }


    @Test
    void getUserById_shouldReturnUser() {

        User user = new User();
        user.setId(1L);
        user.setName("Test User");

        when(userService.getUserById(1L))
                .thenReturn(user);

        User result =
                userController.getUserById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test User", result.getName());

        verify(userService).getUserById(1L);
    }


    @Test
    void updateUser_shouldReturnUser() {

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setName("Updated User");

        when(userService.updateUser(
                eq(1L),
                any(User.class)))
                .thenReturn(updatedUser);

        User result =
                userController.updateUser(
                        1L,
                        updatedUser
                );

        assertNotNull(result);
        assertEquals("Updated User", result.getName());

        verify(userService).updateUser(
                1L,
                updatedUser
        );
    }


    @Test
    void deleteUser_shouldReturnSuccessMessage() {

        doNothing()
                .when(userService)
                .deleteUser(1L);

        String result =
                userController.deleteUser(1L);

        assertEquals(
                "User deleted successfully",
                result
        );

        verify(userService).deleteUser(1L);
    }
}