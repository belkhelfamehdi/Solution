package com.ynov.reactivepipeline.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ynov.reactivepipeline.model.OrderRequest;
import com.ynov.reactivepipeline.model.OrderStatus;
import com.ynov.reactivepipeline.repository.ProductRepository;

import reactor.test.StepVerifier;

class OrderServiceTest {

    private ProductRepository productRepository;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        productRepository = new ProductRepository();
        productRepository.setRandomErrorRate(0.0);
        orderService = new OrderService(productRepository);
    }

    @Test
    void test_processOrderSuccess() {
        OrderRequest request = new OrderRequest(Arrays.asList("PROD001", "PROD002"), "CUST001");

        StepVerifier.create(orderService.processOrder(request))
            .assertNext(order -> {
                assertThat(order.getOrderId()).isNotNull();
                assertThat(order.getProducts()).hasSize(2);
                assertThat(order.getTotalPrice()).isEqualByComparingTo("1125.00");
                assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
            })
            .verifyComplete();
    }

    @Test
    void test_processOrderWithInvalidIds() {
        OrderRequest request = new OrderRequest(Arrays.asList("PROD001", "", null, "UNKNOWN", "PROD003"), "CUST001");

        StepVerifier.create(orderService.processOrder(request))
            .assertNext(order -> {
                assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
                assertThat(order.getProducts()).hasSize(2);
                List<String> ids = order.getProducts().stream()
                    .map(productWithPrice -> productWithPrice.getProduct().getId())
                    .collect(Collectors.toList());
                assertThat(ids).containsExactlyInAnyOrder("PROD001", "PROD003");
            })
            .verifyComplete();
    }

    @Test
    void test_processOrderWithoutStock() {
        OrderRequest request = new OrderRequest(Arrays.asList("PROD004", "PROD005", "PROD003"), "CUST001");

        StepVerifier.create(orderService.processOrder(request))
            .assertNext(order -> {
                assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
                assertThat(order.getProducts()).hasSize(1);
                assertThat(order.getProducts().get(0).getProduct().getId()).isEqualTo("PROD003");
            })
            .verifyComplete();
    }

    @Test
    void test_processOrderWithDiscounts() {
        OrderRequest request = new OrderRequest(Arrays.asList("PROD001", "PROD003"), "CUST001");

        StepVerifier.create(orderService.processOrder(request))
            .assertNext(order -> {
                assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
                assertThat(order.getProducts()).hasSize(2);

                var byId = order.getProducts().stream()
                    .collect(Collectors.toMap(p -> p.getProduct().getId(), p -> p));

                assertThat(byId.get("PROD001").getDiscountPercentage()).isEqualTo(10);
                assertThat(byId.get("PROD003").getDiscountPercentage()).isEqualTo(5);

                BigDecimal sum = order.getProducts().stream()
                    .map(p -> p.getFinalPrice())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                assertThat(order.getTotalPrice()).isEqualByComparingTo(sum);
            })
            .verifyComplete();
    }

    @Test
    void test_processOrderTimeout() {
        productRepository.setBaseDelay(Duration.ofSeconds(6));
        OrderRequest request = new OrderRequest(List.of("PROD001"), "CUST001");

        StepVerifier.create(orderService.processOrder(request))
            .assertNext(order -> {
                assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
                assertThat(order.getProducts()).isEmpty();
                assertThat(order.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
            })
            .verifyComplete();
    }

    @Test
    void test_processOrderWithErrors() {
        productRepository.setRandomErrorRate(0.0);
        productRepository.setForcedFailurePredicate(productId -> "PROD001".equals(productId) || "PROD003".equals(productId));
        OrderRequest request = new OrderRequest(List.of("PROD001", "PROD002", "PROD003", "PROD004"), "CUST001");

        StepVerifier.create(orderService.processOrder(request))
            .assertNext(order -> {
                assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
                assertThat(order.getProducts()).hasSize(1);
                assertThat(order.getProducts().get(0).getProduct().getId()).isEqualTo("PROD002");
            })
            .verifyComplete();
    }
}
