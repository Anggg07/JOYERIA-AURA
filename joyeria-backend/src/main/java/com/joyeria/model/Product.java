package com.joyeria.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(length = 4000)
    private String description;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(name = "compare_price", precision = 12, scale = 2)
    private BigDecimal comparePrice;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private Integer stock = 0;

    private String weight;

    private String dimensions;

    private String size;

    private String color;

    @Column(name = "care_instructions", length = 2000)
    private String careInstructions;

    @Column(name = "features", length = 2000)
    private String features;

    @Column(name = "delivery_time")
    private String deliveryTime;

    @Column(nullable = false)
    private Boolean featured = false;

    @Column(name = "is_new", nullable = false)
    private Boolean isNew = false;

    @Column(name = "best_seller", nullable = false)
    private Boolean bestSeller = false;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "sold_count", nullable = false)
    private Integer soldCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "product_materials",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "material_id")
    )
    private Set<Material> materials = new HashSet<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Inventory> inventoryRecords = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public BigDecimal getDiscountPercent() {
        if (comparePrice == null || comparePrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.ONE.subtract(price.divide(comparePrice, 4, java.math.RoundingMode.HALF_UP))
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, java.math.RoundingMode.HALF_UP);
    }
}