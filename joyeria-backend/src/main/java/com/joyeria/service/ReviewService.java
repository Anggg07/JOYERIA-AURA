package com.joyeria.service;

import com.joyeria.dto.*;
import com.joyeria.exception.ResourceNotFoundException;
import com.joyeria.model.*;
import com.joyeria.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public PagedResponse<ReviewDTO> getProductReviews(Long productId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Review> reviews = reviewRepository.findByProductIdAndStatusOrderByCreatedAtDesc(
                productId, Review.ReviewStatus.APPROVED, pageable);
        return mapToPagedResponse(reviews);
    }

    public List<ReviewDTO> getRecentApproved(int limit) {
        return reviewRepository.findByStatus(Review.ReviewStatus.APPROVED, PageRequest.of(0, limit))
                .getContent().stream().map(this::mapToDTO).toList();
    }

    public PagedResponse<ReviewDTO> getReviewsForAdmin(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Review> reviews = (status == null || status.isBlank())
                ? reviewRepository.findAllByOrderByCreatedAtDesc(pageable)
                : reviewRepository.findByStatus(Review.ReviewStatus.valueOf(status), pageable);
        return mapToPagedResponse(reviews);
    }

    @Transactional
    public ReviewDTO createReview(String email, ReviewCreateRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        if (reviewRepository.existsByUserIdAndProductId(user.getId(), product.getId())) {
            throw new IllegalArgumentException("Ya has reseñado este producto");
        }

        boolean purchased = orderRepository.existsByUserIdAndItemsProductIdAndStatusIn(
                user.getId(), product.getId(),
                List.of(Order.OrderStatus.PAID, Order.OrderStatus.PREPARING,
                        Order.OrderStatus.SHIPPED, Order.OrderStatus.DELIVERED));
        if (!purchased) {
            throw new IllegalArgumentException("Solo puedes reseñar productos que hayas comprado");
        }

        Review review = new Review();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(request.getRating());
        review.setTitle(request.getTitle());
        review.setComment(request.getComment());
        review.setStatus(Review.ReviewStatus.PENDING);

        return mapToDTO(reviewRepository.save(review));
    }

    @Transactional
    public ReviewDTO moderateReview(Long id, String status) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reseña no encontrada"));
        review.setStatus(Review.ReviewStatus.valueOf(status));
        return mapToDTO(reviewRepository.save(review));
    }

    public boolean hasUserReviewed(String email, Long productId) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return false;
        return reviewRepository.existsByUserIdAndProductId(user.getId(), productId);
    }

    private ReviewDTO mapToDTO(Review review) {
        return ReviewDTO.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .productName(review.getProduct().getName())
                .userId(review.getUser().getId())
                .userName(review.getUser().getFullName())
                .rating(review.getRating())
                .title(review.getTitle())
                .comment(review.getComment())
                .status(review.getStatus())
                .createdAt(review.getCreatedAt())
                .build();
    }

    private PagedResponse<ReviewDTO> mapToPagedResponse(Page<Review> reviews) {
        return PagedResponse.<ReviewDTO>builder()
                .content(reviews.getContent().stream().map(this::mapToDTO).toList())
                .page(reviews.getNumber())
                .size(reviews.getSize())
                .totalElements(reviews.getTotalElements())
                .totalPages(reviews.getTotalPages())
                .first(reviews.isFirst())
                .last(reviews.isLast())
                .build();
    }
}
