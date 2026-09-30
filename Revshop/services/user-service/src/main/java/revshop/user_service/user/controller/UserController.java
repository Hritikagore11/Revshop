package revshop.user_service.user.controller;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import revshop.user_service.user.model.User;
import revshop.user_service.user.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public User register(@RequestBody User user){
        return userService.register(user);
    }

    @PostMapping("/login")
    public String login(
            @RequestParam String email,
            @RequestParam String password) {

        return userService.login(email, password);
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody User user){
        return userService.updateUser(id, user);
    }

    @DeleteMapping("/{id}")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return "User deleted successfully";
    }
    @GetMapping("/profile")
    public User getProfile(Authentication authentication) {
        return userService.getProfile(authentication);
    }
    @PutMapping("/profile")
    public User updateProfile(
            @RequestBody User user,
            Authentication authentication) {

        return userService.updateProfile(authentication, user);
    }
}