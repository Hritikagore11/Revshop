package revshop.user.model;
import org.springframework.security.crypto.password.PasswordEncoder;
import revshop.security.JwtService;
import revshop.user.model.LoginResponse;

public class LoginResponse {
    private String message;
    private String token;
    private String role;
    private Long userId;


    public LoginResponse(String message, String token, String role, Long userId) {
        this.message = message;
        this.token = token;
        this.role = role;
        this.userId = userId;

    }

    public String getMessage() {
        return message;
    }

    public String getToken() {
        return token;
    }

    public String getRole() {
        return role;
    }

    public Long getUserId() {
        return userId;
    }
}