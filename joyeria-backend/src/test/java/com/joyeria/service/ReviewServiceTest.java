package com.joyeria.service;

import com.joyeria.dto.ReviewCreateRequest;
import com.joyeria.exception.ResourceNotFoundException;
import com.joyeria.model.Order;
import com.joyeria.model.Product;
import com.joyeria.model.Review;
import com.joyeria.model.User;
import com.joyeria.repository.OrderRepository;
import com.joyeria.repository.ProductRepository;
import com.joyeria.repository.ReviewRepository;
import com.joyeria.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private ReviewService reviewService;

    private User user;
    private Product product;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("buyer@example.com");
        user.setFirstName("Buy");
        user.setLastName("Er");

        product = new Product();
        product.setId(5L);
        product.setName("Anillo oro");
    }

    @Test
    void createReview_requiresPurchase() {
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(reviewRepository.existsByUserIdAndProductId(1L, 5L)).thenReturn(false);
        when(orderRepository.existsByUserIdAndItemsProductIdAndStatusIn(eq(1L), eq(5L), any()))
                .thenReturn(false);

        ReviewCreateRequest request = new ReviewCreateRequest();
        request.setProductId(5L);
        request.setRating(5);

        assertThrows(IllegalArgumentException.class,
                () -> reviewService.createReview("buyer@example.com", request));
    }

    @Test
    void createReview_pendingByDefaultWhenPurchased() {
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(reviewRepository.existsByUserIdAndProductId(1L, 5L)).thenReturn(false);
        when(orderRepository.existsByUserIdAndItemsProductIdAndStatusIn(eq(1L), eq(5L),
                eq(List.of(Order.OrderStatus.PAID, Order.OrderStatus.PREPARING,
                        Order.OrderStatus.SHIPPED, Order.OrderStatus.DELIVERED))))
                .thenReturn(true);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewCreateRequest request = new ReviewCreateRequest();
        request.setProductId(5L);
        request.setRating(4);
        request.setTitle("Buena pieza");
        request.setComment("Llegó rápido");

        var saved = reviewService.createReview("buyer@example.com", request);

        assertEquals(Review.ReviewStatus.PENDING, saved.getStatus());
    }

    @Test
    void createReview_unknownProductNotFound() {
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));
        when(productRepository.findById(5L)).thenReturn(Optional.empty());

        ReviewCreateRequest request = new ReviewCreateRequest();
        request.setProductId(5L);
        request.setRating(5);

        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.createReview("buyer@example.com", request));
    }

    @Test
    void getReviewsForAdmin_WithStatus_FiltersByStatus() {
        Review review = new Review();
        review.setId(1L);
        review.setUser(user);
        review.setProduct(product);
        review.setRating(5);
        review.setStatus(Review.ReviewStatus.PENDING);

        when(reviewRepository.findByStatus(eq(Review.ReviewStatus.PENDING), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(review)));

        var response = reviewService.getReviewsForAdmin("PENDING", 0, 20);

        assertEquals(1, response.getContent().size());
        assertEquals(Review.ReviewStatus.PENDING, response.getContent().get(0).getStatus());
    }

    @Test
    void getReviewsForAdmin_WithoutStatus_ReturnsAll() {
        when(reviewRepository.findAllByOrderByCreatedAtDesc(any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        var response = reviewService.getReviewsForAdmin(null, 0, 20);

        assertTrue(response.getContent().isEmpty());
    }
}
