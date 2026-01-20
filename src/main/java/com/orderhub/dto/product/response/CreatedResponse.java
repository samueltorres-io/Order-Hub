package com.orderhub.dto.product.response;

import java.math.BigDecimal;
import java.util.UUID;

import com.orderhub.enums.ProductStatus;

public record CreatedResponse(

    UUID id,
    String name,
    String description,
    BigDecimal price,
    ProductStatus status

) {}
