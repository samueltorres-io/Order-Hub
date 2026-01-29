package com.orderhub.controller;

import java.util.UUID;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orderhub.dto.error.ApiError;
import com.orderhub.dto.order.request.CreateOrderRequest;
import com.orderhub.dto.order.response.OrderResponse;
import com.orderhub.entity.User;
import com.orderhub.security.CurrentUser;
import com.orderhub.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Operations related to order management")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Create a new Order", description = "Creates a new order for the authenticated user.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Order created successfully"),
        @ApiResponse(responseCode = "400", description = "Bad Request or Internal Business Validation", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class),
            examples = @ExampleObject(value = """
                {
                    "success": false,
                    "errorCode": "INTERNAL_SERVER_ERROR",
                    "status": 400,
                    "message": "Product count mismatch or Invalid Total",
                    "timestamp": "2024-01-24T10:00:00Z"
                }
            """))
        ),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    @PostMapping
    public ResponseEntity<OrderResponse> create(
        @Parameter(hidden = true) @CurrentUser User user,
        @RequestBody @Valid CreateOrderRequest req
    ) {
        var response = orderService.create(user, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get Order by ID", description = "Retrieves order details. Requires user to be the owner or an Admin.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Order not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class),
            examples = @ExampleObject(value = """
                {
                    "success": false,
                    "errorCode": "ORDER_NOT_FOUND",
                    "status": 404,
                    "message": "Order not found",
                    "timestamp": "2024-01-24T10:05:00Z"
                }
            """))
        ),
        @ApiResponse(responseCode = "401", description = "Unauthorized access (Not owner/admin)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class),
            examples = @ExampleObject(value = """
                {
                    "success": false,
                    "errorCode": "UNAUTHORIZED",
                    "status": 401,
                    "message": "Access denied",
                    "timestamp": "2024-01-24T10:05:00Z"
                }
            """))
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(
        @Parameter(hidden = true) @CurrentUser User user,
        @PathVariable UUID orderId
    ) {
        var response = orderService.getOrderById(user.getId(), orderId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List all Orders (Admin)", description = "Retrieves a paginated list of all orders. Requires ADMIN role.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "List retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized (Not Admin)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class),
            examples = @ExampleObject(value = """
                {
                    "success": false,
                    "errorCode": "UNAUTHORIZED",
                    "status": 401,
                    "message": "Admin privileges required",
                    "timestamp": "2024-01-24T10:10:00Z"
                }
            """))
        )
    })
    @GetMapping("/admin")
    public ResponseEntity<Page<OrderResponse>> getAll(
        @ParameterObject @PageableDefault(page = 0, size = 20) Pageable pageable,
        @Parameter(hidden = true) @CurrentUser User user
    ) {
        var response = orderService.getAll(user, pageable);
        return ResponseEntity.ok(response);
    }

}