package com.revshop.revshop.controller;

import com.revshop.revshop.model.Review;
import com.revshop.revshop.service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products/{productId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public Review addReview(
            @PathVariable Long productId,
            @RequestBody Review review) {

        return reviewService.addReview(productId, review);
    }

    @GetMapping
    public List<Review> getReviewsByProduct(
            @PathVariable Long productId) {

        return reviewService.getReviewsByProduct(productId);
    }
}