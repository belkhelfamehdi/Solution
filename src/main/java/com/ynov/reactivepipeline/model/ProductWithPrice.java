package com.ynov.reactivepipeline.model;

import java.math.BigDecimal;
import java.util.Objects;

public class ProductWithPrice {
    private final Product product;
    private final BigDecimal originalPrice;
    private final Integer discountPercentage;
    private final BigDecimal finalPrice;

    public ProductWithPrice(Product product, BigDecimal originalPrice, Integer discountPercentage, BigDecimal finalPrice) {
        this.product = Objects.requireNonNull(product, "product is required");
        this.originalPrice = Objects.requireNonNull(originalPrice, "originalPrice is required");
        this.discountPercentage = Objects.requireNonNull(discountPercentage, "discountPercentage is required");
        this.finalPrice = Objects.requireNonNull(finalPrice, "finalPrice is required");
    }

    public Product getProduct() {
        return product;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public Integer getDiscountPercentage() {
        return discountPercentage;
    }

    public BigDecimal getFinalPrice() {
        return finalPrice;
    }
}
