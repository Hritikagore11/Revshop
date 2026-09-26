package revshop.product_service.product.service;

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

    public Review addReview(Review review) {

        if (review.getRating() < 1 || review.getRating() > 5) {
            throw new RuntimeException("Rating must be between 1 and 5");
        }

        if (review.getProductId() == null) {
            throw new RuntimeException("Product ID is required");
        }

        if (review.getBuyerId() == null) {
            throw new RuntimeException("Buyer ID is required");
        }

        if (reviewRepository.existsByProductIdAndBuyerId(
                review.getProductId(),
                review.getBuyerId())) {

            throw new RuntimeException(
                    "Buyer has already reviewed this product");
        }

        return reviewRepository.save(review);
    }

    public List<Review> getReviewsByProduct(Long productId) {
        return reviewRepository.findByProductId(productId);
    }
}