package com.ynov.reactivepipeline.model;

import java.util.List;
import java.util.Objects;

public class OrderRequest {
    private final List<String> productIds;
    private final String customerId;

    public OrderRequest(List<String> productIds, String customerId) {
        this.productIds = Objects.requireNonNull(productIds, "productIds is required");
        this.customerId = customerId;
    }

    public List<String> getProductIds() {
        return productIds;
    }

    public String getCustomerId() {
        return customerId;
    }
}
