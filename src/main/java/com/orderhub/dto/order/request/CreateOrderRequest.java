package com.orderhub.dto.order.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(

    @NotEmpty(message = "The order must contain at least one item")
    @Valid
    List<OrderItemRequest> items

) {
    public record OrderItemRequest(

        @NotNull(message = "Product Id is mandatory")
        UUID productId,

        @Min(value = 1, message = "The quantity must be at least 1")
        Integer quantity
    ) {}
}
