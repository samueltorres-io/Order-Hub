package com.orderhub.dto.product.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(

    UUID id,
    String name,
    String description,
    BigDecimal price

) {}