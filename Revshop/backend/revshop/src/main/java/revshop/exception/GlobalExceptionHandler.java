package revshop.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserNotFound(
            UserNotFoundException ex) {

        return buildResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleProductNotFound(
            ProductNotFoundException ex) {

        return buildResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(CartNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleCartNotFound(
            CartNotFoundException ex) {

        return buildResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(CartItemNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleCartItemNotFound(
            CartItemNotFoundException ex) {

        return buildResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidQuantityException.class)
    public ResponseEntity<Map<String, String>> handleInvalidQuantity(
            InvalidQuantityException ex) {

        return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Map<String, String>> handleInsufficientStock(
            InsufficientStockException ex) {

        return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleOrderNotFound(
            OrderNotFoundException ex) {

        return buildResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(CartEmptyException.class)
    public ResponseEntity<Map<String, String>> handleCartEmpty(
            CartEmptyException ex) {

        return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<Map<String, String>> buildResponse(
            String message,
            HttpStatus status) {

        Map<String, String> response = new HashMap<>();
        response.put("error", message);

        return ResponseEntity.status(status).body(response);
    }
}