package com.ynov.reactivepipeline.repository;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

import com.ynov.reactivepipeline.model.Product;

import jakarta.enterprise.context.ApplicationScoped;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ApplicationScoped
public class ProductRepository {

    private final Map<String, Product> productStore = Map.of(
        "PROD001", new Product("PROD001", "Laptop Pro", new BigDecimal("1200.00"), 10, "ELECTRONICS"),
        "PROD002", new Product("PROD002", "Wireless Mouse", new BigDecimal("50.00"), 15, "ELECTRONICS"),
        "PROD003", new Product("PROD003", "Coffee Mug", new BigDecimal("12.00"), 30, "HOME"),
        "PROD004", new Product("PROD004", "Notebook", new BigDecimal("5.00"), 0, "OFFICE"),
        "PROD005", new Product("PROD005", "Headphones", new BigDecimal("80.00"), 0, "ELECTRONICS")
    );

    private final Random random = new Random();
    private Duration baseDelay = Duration.ofMillis(100);
    private double randomErrorRate = 0.10;
    private Predicate<String> forcedFailurePredicate = id -> false;

    public Mono<Product> findById(String id) {
        return simulateLatency()
            .then(maybeFail(id))
            .then(Mono.justOrEmpty(productStore.get(id)));
    }

    public Flux<Product> findByIds(List<String> ids) {
        return Flux.fromIterable(ids)
            .flatMap(this::findById);
    }

    public Mono<Integer> getStock(String productId) {
        return simulateLatency()
            .then(maybeFail(productId))
            .then(Mono.justOrEmpty(productStore.get(productId)))
            .map(Product::getStock)
            .defaultIfEmpty(0);
    }

    public void setBaseDelay(Duration baseDelay) {
        this.baseDelay = baseDelay;
    }

    public void setRandomErrorRate(double randomErrorRate) {
        this.randomErrorRate = randomErrorRate;
    }

    public void setForcedFailurePredicate(Predicate<String> forcedFailurePredicate) {
        this.forcedFailurePredicate = forcedFailurePredicate;
    }

    private Mono<Void> maybeFail(String key) {
        if (forcedFailurePredicate.test(key) || random.nextDouble() < randomErrorRate) {
            return Mono.error(new RuntimeException("Simulated repository error for: " + key));
        }
        return Mono.empty();
    }

    private Mono<Long> simulateLatency() {
        return Mono.delay(baseDelay);
    }
}
