package revshop.product_service.product.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import revshop.product_service.product.model.Review;
import revshop.product_service.product.service.ReviewService;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(
            ReviewService reviewService) {

        this.reviewService = reviewService;
    }

    @PostMapping("/{productId}/reviews")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<Review> addReview(
            @PathVariable Long productId,
            @RequestBody Review review,
            Authentication authentication) {

        Long buyerId =
                reviewService.getAuthenticatedUserId(
                        authentication
                );

        Review savedReview =
                reviewService.addReview(
                        productId,
                        buyerId,
                        review.getRating(),
                        review.getComment()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedReview);
    }

    @GetMapping("/{productId}/reviews")
    @PreAuthorize("hasAnyRole('BUYER', 'SELLER')")
    public ResponseEntity<List<Review>> getReviews(
            @PathVariable Long productId) {

        return ResponseEntity.ok(
                reviewService.getReviewsByProduct(productId)
        );
    }

    @PutMapping("/reviews/{reviewId}")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<Review> updateReview(
            @PathVariable Long reviewId,
            @RequestBody Review review,
            Authentication authentication) {

        Long buyerId =
                reviewService.getAuthenticatedUserId(
                        authentication
                );

        Review updatedReview =
                reviewService.updateReview(
                        reviewId,
                        buyerId,
                        review.getRating(),
                        review.getComment()
                );

        return ResponseEntity.ok(updatedReview);
    }

    @DeleteMapping("/reviews/{reviewId}")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long reviewId,
            Authentication authentication) {

        Long buyerId =
                reviewService.getAuthenticatedUserId(
                        authentication
                );

        reviewService.deleteReview(
                reviewId,
                buyerId
        );

        return ResponseEntity.noContent().build();
    }
}