package com.orderhub.dto.order.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.orderhub.enums.OrderStatus;

public record OrderResponse(
    UUID orderId,
    BigDecimal total,
    OrderStatus status,
    Instant createdAt,
    List<OrderItemResponse> items
) {

    public record OrderItemResponse(
        UUID productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subTotal
    ) {}
}