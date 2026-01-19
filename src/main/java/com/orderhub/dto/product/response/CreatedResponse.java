package com.orderhub.dto.product.response;

import java.util.UUID;

public record CreatedResponse(

    UUID id,
    String name,
    boolean status

) {}
