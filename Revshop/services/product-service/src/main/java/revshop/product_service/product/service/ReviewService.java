package revshop.product_service.product.service;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import revshop.product_service.product.model.Review;
import revshop.product_service.product.repository.ReviewRepository;

import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;

    public ReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    // =========================
    // GET AUTHENTICATED USER ID
    // =========================

    public Long getAuthenticatedUserId(
            Authentication authentication) {

        if (authentication == null ||
                authentication.getDetails() == null) {

            throw new RuntimeException(
                    "User is not authenticated"
            );
        }

        Object details = authentication.getDetails();

        if (!(details instanceof Long userId)) {

            throw new RuntimeException(
                    "Unable to identify authenticated user"
            );
        }

        return userId;
    }

    // =========================
    // ADD REVIEW
    // =========================

    public Review addReview(
            Long productId,
            Long buyerId,
            int rating,
            String comment) {

        validateRating(rating);

        if (productId == null) {
            throw new RuntimeException(
                    "Product ID is required"
            );
        }

        if (buyerId == null) {
            throw new RuntimeException(
                    "Buyer ID is required"
            );
        }

        if (reviewRepository.existsByProductIdAndBuyerId(
                productId,
                buyerId)) {

            throw new RuntimeException(
                    "Buyer has already reviewed this product"
            );
        }

        Review review = new Review();

        review.setProductId(productId);
        review.setBuyerId(buyerId);
        review.setRating(rating);
        review.setComment(comment);

        return reviewRepository.save(review);
    }

    // =========================
    // GET REVIEWS
    // =========================

    public List<Review> getReviewsByProduct(
            Long productId) {

        return reviewRepository.findByProductId(productId);
    }

    // =========================
    // UPDATE REVIEW
    // =========================

    public Review updateReview(
            Long reviewId,
            Long buyerId,
            int rating,
            String comment) {

        validateRating(rating);

        Review review =
                reviewRepository.findById(reviewId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Review not found"
                                ));

        if (!review.getBuyerId().equals(buyerId)) {

            throw new RuntimeException(
                    "You are not authorized to update this review"
            );
        }

        review.setRating(rating);
        review.setComment(comment);

        return reviewRepository.save(review);
    }

    // =========================
    // DELETE REVIEW
    // =========================

    public void deleteReview(
            Long reviewId,
            Long buyerId) {

        Review review =
                reviewRepository.findById(reviewId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Review not found"
                                ));

        if (!review.getBuyerId().equals(buyerId)) {

            throw new RuntimeException(
                    "You are not authorized to delete this review"
            );
        }

        reviewRepository.delete(review);
    }

    // =========================
    // RATING VALIDATION
    // =========================

    private void validateRating(int rating) {

        if (rating < 1 || rating > 5) {

            throw new RuntimeException(
                    "Rating must be between 1 and 5"
            );
        }
    }
}