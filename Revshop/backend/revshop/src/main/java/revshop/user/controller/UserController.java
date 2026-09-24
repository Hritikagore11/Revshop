package revshop.user.controller;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import revshop.user.model.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import revshop.user.model.User;
import revshop.user.service.UserService;
import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public User register(@Valid @RequestBody User user){
        return userService.register(user);
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestParam String email, @RequestParam String password){
        return userService.login(email, password);
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id, Authentication authentication) throws AccessDeniedException {
        User loggedInUser = (User) authentication.getPrincipal();

        if(!loggedInUser.getId().equals(id)){
            throw new AccessDeniedException("You can only access your own profile");
        }
        return userService.getUserById(id);
    }

    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody User user, Authentication authentication) throws AccessDeniedException {
        User loggedInUser = (User) authentication.getPrincipal();

        if(!loggedInUser.getId().equals(id)){
            throw new AccessDeniedException("You can only update your own profile");
        }

        return userService.updateUser(id, user);
    }

    @DeleteMapping("/{id}")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return "User deleted successfully";
    }
}