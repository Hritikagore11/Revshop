package revshop.product_service.product.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import org.springframework.web.server.ResponseStatusException;
import revshop.product_service.product.model.Review;
import revshop.product_service.product.repository.ReviewRepository;

import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;

    public ReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    public Long getAuthenticatedUserId(
            Authentication authentication) {

        if (authentication == null ||
                authentication.getDetails() == null) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "User is not authenticated"
            );
        }

        Object details = authentication.getDetails();

        if (!(details instanceof Long userId)) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Unable to identify authenticated user"
            );
        }

        return userId;
    }


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

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
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


    public List<Review> getReviewsByProduct(
            Long productId) {

        return reviewRepository.findByProductId(productId);
    }


    public Review updateReview(
            Long reviewId,
            Long buyerId,
            int rating,
            String comment) {

        validateRating(rating);

        Review review =
                reviewRepository.findById(reviewId)
                        .orElseThrow(() ->
         new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Review not found"
         ));

        if (!review.getBuyerId().equals(buyerId)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not authorized to update this review"
            );
        }

        review.setRating(rating);
        review.setComment(comment);

        return reviewRepository.save(review);
    }

    public void deleteReview(
            Long reviewId,
            Long buyerId) {

        Review review =
                reviewRepository.findById(reviewId)
                        .orElseThrow(() ->
        new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Review not found"
        ));

        if (!review.getBuyerId().equals(buyerId)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not authorized to update this review"
            );
        }

        reviewRepository.delete(review);
    }


    private void validateRating(int rating) {

        if (rating < 1 || rating > 5) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Rating must be between 1 and 5"
            );
        }
    }
}