package com.orderhub.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.orderhub.dto.error.ApiError;
import com.orderhub.dto.product.request.CreateRequest;
import com.orderhub.dto.product.request.UpdateRequest;
import com.orderhub.dto.product.response.CreatedResponse;
import com.orderhub.dto.product.response.ProductResponse;
import com.orderhub.entity.User;
import com.orderhub.security.CurrentUser;
import com.orderhub.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Create new resource", description = "Creates a new resource in the system. Requires ADMIN privileges.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Resource created successfully",
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = CreatedResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid input data",
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(
                    name = "Invalid Input",
                    value = "{\"success\":false,\"errorCode\":\"ERR_INVALID_INPUT\",\"status\":400,\"message\":\"Invalid input data\",\"timestamp\":\"2024-01-24T10:00:00Z\",\"traceId\":\"req-001\",\"details\":[\"name: must not be blank\"]}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Unauthorized",
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(
                    name = "Unauthorized",
                    summary = "Authentication failed or missing",
                    value = "{\"success\":false,\"errorCode\":\"ERR_UNAUTHORIZED\",\"status\":401,\"message\":\"Unauthorized\",\"timestamp\":\"2024-01-24T10:00:00Z\",\"traceId\":\"sec-401\",\"details\":null}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Related resource not found",
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(
                    name = "User Not Found",
                    summary = "The user context or related entity was not found",
                    value = "{\"success\":false,\"errorCode\":\"ERR_USER_NOT_FOUND\",\"status\":404,\"message\":\"User not found\",\"timestamp\":\"2024-01-24T10:00:00Z\",\"traceId\":\"db-404\",\"details\":null}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "409", 
            description = "Resource duplication",
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(
                    name = "Duplicated Resource",
                    summary = "Resource with unique constraint already exists",
                    value = "{\"success\":false,\"errorCode\":\"ERR_DUPLICATED_RESOURCE\",\"status\":409,\"message\":\"Resource already exists\",\"timestamp\":\"2024-01-24T10:00:00Z\",\"traceId\":\"db-409\",\"details\":null}"
                )
            )
        )
    })
    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    public ResponseEntity<CreatedResponse> create(
        @Parameter(hidden = true) @CurrentUser User user,
        @Valid @RequestBody CreateRequest req
    ) {
        var response = productService.create(user, req);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update existing resource", description = "Updates a resource by ID. Requires ADMIN privileges.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Resource updated successfully",
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = CreatedResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Unauthorized",
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(
                    name = "Unauthorized",
                    value = "{\"success\":false,\"errorCode\":\"ERR_UNAUTHORIZED\",\"status\":401,\"message\":\"Authentication failed\",\"timestamp\":\"2024-01-24T10:00:00Z\",\"traceId\":\"sec-401\",\"details\":null}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Resource not found",
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = ApiError.class),
                examples = {
                    @ExampleObject(
                        name = "Product Not Found",
                        summary = "Target product ID does not exist",
                        value = "{\"success\":false,\"errorCode\":\"ERR_PRODUCT_NOT_FOUND\",\"status\":404,\"message\":\"Product not found\",\"timestamp\":\"2024-01-24T10:00:00Z\",\"traceId\":\"prd-404\",\"details\":null}"
                    ),
                    @ExampleObject(
                        name = "User Not Found",
                        summary = "Related user context not found",
                        value = "{\"success\":false,\"errorCode\":\"ERR_USER_NOT_FOUND\",\"status\":404,\"message\":\"User not found\",\"timestamp\":\"2024-01-24T10:00:00Z\",\"traceId\":\"usr-404\",\"details\":null}"
                    )
                }
            )
        )
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    public ResponseEntity<CreatedResponse> update(
        @Parameter(hidden = true) @CurrentUser User user,
        @PathVariable UUID id,
        @Valid @RequestBody UpdateRequest req
    ) {
        var response = productService.update(user, id, req);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Get product by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable UUID id) {
        var response = productService.getById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get product by name", description = "Retrieves a specific product details using its exact name")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Product found",
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = ProductResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid name format",
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(
                    name = "Invalid Input",
                    value = "{\"success\":false,\"errorCode\":\"ERR_INVALID_INPUT\",\"status\":400,\"message\":\"Invalid input provided\",\"timestamp\":\"2024-01-24T10:00:00Z\",\"traceId\":\"req-002\",\"details\":[\"name: invalid format\"]}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Product not found",
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(
                    name = "Product Not Found",
                    value = "{\"success\":false,\"errorCode\":\"ERR_PRODUCT_NOT_FOUND\",\"status\":404,\"message\":\"Product not found\",\"timestamp\":\"2024-01-24T10:00:00Z\",\"traceId\":\"prd-404\",\"details\":null}"
                )
            )
        )
    })
    @GetMapping("/search") 
    public ResponseEntity<ProductResponse> getByName(@RequestParam String name) {
        var response = productService.getByName(name);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get all products (Paginated)", 
        description = "Retrieves a paginated list of all available products. Accepts query params: page, size, sort. Returns empty page if no results found."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Page retrieved successfully",
            useReturnTypeSchema = true
        )
    })
    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getProducts(
        @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        var response = productService.getProducts(pageable);
        return ResponseEntity.ok(response);
    }
}
