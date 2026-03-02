package com.ynov.reactivepipeline.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class Order {
    private final String orderId;
    private final List<String> productIds;
    private final List<ProductWithPrice> products;
    private final BigDecimal totalPrice;
    private final Boolean discountApplied;
    private final LocalDateTime createdAt;
    private final OrderStatus status;

    public Order(String orderId,
                 List<String> productIds,
                 List<ProductWithPrice> products,
                 BigDecimal totalPrice,
                 Boolean discountApplied,
                 LocalDateTime createdAt,
                 OrderStatus status) {
        this.orderId = Objects.requireNonNull(orderId, "orderId is required");
        this.productIds = Objects.requireNonNull(productIds, "productIds is required");
        this.products = Objects.requireNonNull(products, "products is required");
        this.totalPrice = Objects.requireNonNull(totalPrice, "totalPrice is required");
        this.discountApplied = Objects.requireNonNull(discountApplied, "discountApplied is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.status = Objects.requireNonNull(status, "status is required");
    }

    public String getOrderId() {
        return orderId;
    }

    public List<String> getProductIds() {
        return productIds;
    }

    public List<ProductWithPrice> getProducts() {
        return products;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public Boolean getDiscountApplied() {
        return discountApplied;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public OrderStatus getStatus() {
        return status;
    }
}
