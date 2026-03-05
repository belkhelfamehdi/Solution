package com.ynov.reactivepipeline.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ynov.reactivepipeline.exception.InvalidOrderException;
import com.ynov.reactivepipeline.exception.ProductNotFoundException;
import com.ynov.reactivepipeline.model.Order;
import com.ynov.reactivepipeline.model.OrderRequest;
import com.ynov.reactivepipeline.model.OrderStatus;
import com.ynov.reactivepipeline.model.Product;
import com.ynov.reactivepipeline.model.ProductWithPrice;
import com.ynov.reactivepipeline.repository.ProductRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@ApplicationScoped
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final ProductRepository productRepository;

    @Inject
    public OrderService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Mono<Order> processOrder(OrderRequest request) {
        return validateRequest(request)
            .doOnNext(valid -> logger.info("Request validated for customer={}", valid.getCustomerId()))
            .flatMapMany(validRequest -> Flux.fromIterable(
                    validRequest.getProductIds().stream().filter(Objects::nonNull).toList())
                .filter(productId -> productId != null && !productId.isBlank())
                .take(100)
                .doOnNext(productId -> logger.info("Processing productId={}", productId))
                .parallel()
                .runOn(Schedulers.parallel())
                .flatMap(this::findAndCheckStock)
                .sequential()
                .map(this::applyDiscount)
                .doOnNext(p -> logger.info("Discount applied for product={}, finalPrice={}",
                    p.getProduct().getId(), p.getFinalPrice()))
            )
            .collectList()
            .map(productsWithPrice -> createCompletedOrder(request, productsWithPrice))
            .doOnNext(order -> logger.info("Order completed orderId={}, productCount={}",
                order.getOrderId(), order.getProducts().size()))
            .timeout(Duration.ofSeconds(5))
            .doOnError(error -> logger.error("Order processing error", error))
            .onErrorResume(error -> Mono.just(createFailedOrder(request, error)))
            .doFinally(signalType -> logger.info("Order pipeline finished with signal={}", signalType));
    }

    private Mono<OrderRequest> validateRequest(OrderRequest request) {
        if (request == null) {
            return Mono.error(new InvalidOrderException("OrderRequest must not be null"));
        }
        if (request.getProductIds() == null || request.getProductIds().isEmpty()) {
            return Mono.error(new InvalidOrderException("Product IDs list must not be empty"));
        }
        if (request.getCustomerId() == null || request.getCustomerId().isBlank()) {
            return Mono.error(new InvalidOrderException("Customer ID must not be null or blank"));
        }
        return Mono.just(request);
    }

    private Mono<Product> findAndCheckStock(String productId) {
        return productRepository.findById(productId)
            .switchIfEmpty(Mono.error(new ProductNotFoundException(productId)))
            .flatMap(product -> productRepository.getStock(productId)
                .map(stock -> new AbstractMap.SimpleEntry<>(product, stock)))
            .filter(productAndStock -> productAndStock.getValue() > 0)
            .map(Map.Entry::getKey)
            .doOnNext(product -> logger.info("Product available with stock: {}", product.getId()))
            .doOnError(error -> logger.warn("Skipping productId={} due to error: {}",
                productId, error.getMessage()))
            .onErrorResume(error -> Mono.empty());
    }

    private ProductWithPrice applyDiscount(Product product) {
        int discountPercentage = "ELECTRONICS".equalsIgnoreCase(product.getCategory()) ? 10 : 5;
        BigDecimal finalPrice = product.getPrice()
            .multiply(BigDecimal.valueOf(100 - discountPercentage))
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        return new ProductWithPrice(product, product.getPrice(), discountPercentage, finalPrice);
    }

    private Order createCompletedOrder(OrderRequest request, List<ProductWithPrice> productsWithPrice) {
        BigDecimal totalPrice = productsWithPrice.stream()
            .map(ProductWithPrice::getFinalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean discountApplied = productsWithPrice.stream()
            .anyMatch(product -> product.getDiscountPercentage() > 0);

        return new Order(
            UUID.randomUUID().toString(),
            request.getProductIds(),
            productsWithPrice,
            totalPrice,
            discountApplied,
            LocalDateTime.now(),
            OrderStatus.COMPLETED
        );
    }

    private Order createFailedOrder(OrderRequest request, Throwable throwable) {
        logger.error("Creating FAILED order due to: {}", throwable.getMessage());

        List<String> productIds = request != null && request.getProductIds() != null
            ? request.getProductIds()
            : Collections.emptyList();

        return new Order(
            UUID.randomUUID().toString(),
            productIds,
            Collections.emptyList(),
            BigDecimal.ZERO,
            false,
            LocalDateTime.now(),
            OrderStatus.FAILED
        );
    }
}
