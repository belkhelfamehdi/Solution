package com.ynov.reactivepipeline.model;

import java.math.BigDecimal;
import java.util.Objects;

public class Product {
    private final String id;
    private final String name;
    private final BigDecimal price;
    private final Integer stock;
    private final String category;

    public Product(String id, String name, BigDecimal price, Integer stock, String category) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.name = Objects.requireNonNull(name, "name is required");
        this.price = Objects.requireNonNull(price, "price is required");
        this.stock = Objects.requireNonNull(stock, "stock is required");
        this.category = Objects.requireNonNull(category, "category is required");

        if (this.price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("price must be > 0");
        }
        if (this.stock < 0) {
            throw new IllegalArgumentException("stock must be >= 0");
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Integer getStock() {
        return stock;
    }

    public String getCategory() {
        return category;
    }
}
