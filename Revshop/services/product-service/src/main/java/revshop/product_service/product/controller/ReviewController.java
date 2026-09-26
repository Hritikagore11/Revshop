package revshop.product_service.product.controller;

import org.springframework.web.bind.annotation.*;
import revshop.product_service.product.model.Review;
import revshop.product_service.product.service.ReviewService;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/{productId}/reviews")
    @PreAuthorize("hasRole('BUYER')")
    public Review addReview(
            @PathVariable Long productId,
            @RequestBody Review review) {

        review.setProductId(productId);

        return reviewService.addReview(review);
    }

    @GetMapping("/{productId}/reviews")
    public List<Review> getReviews(
            @PathVariable Long productId) {

        return reviewService.getReviewsByProduct(productId);
    }
}