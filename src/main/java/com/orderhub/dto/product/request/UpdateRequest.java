package com.orderhub.dto.product.request;

import java.math.BigDecimal;

import com.orderhub.enums.ProductStatus;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateRequest(

    @NotBlank(message = "Name is mandatory")
    @Size(min = 3, max = 200, message = "Name must be between 3 and 200 characters long")
    String name,

    @NotBlank(message = "Description is mandatory")
    @Size(min = 3, max = 500, message = "Descripton must be between 3 and 500 characters long")
    String description,

    @NotNull(message = "Price is mandatory")
    @Positive(message = "Price must be greater than zero")
    @Digits(integer = 10, fraction = 2, message = "Price must fit format XXXXXXXXXX.YY")
    BigDecimal price,

    @NotNull(message = "Stock is mandatory")
    @PositiveOrZero(message = "Stock cannot be negative")
    Integer stock,

    @NotNull(message = "Status is mandatory")
    ProductStatus status

) {}
