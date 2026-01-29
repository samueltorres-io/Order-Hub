package com.orderhub.controller;

import java.util.UUID;

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

import com.orderhub.dto.order.request.CreateOrderRequest;
import com.orderhub.dto.order.response.OrderResponse;
import com.orderhub.entity.User;
import com.orderhub.security.CurrentUser;
import com.orderhub.service.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> create(@CurrentUser User user, @RequestBody @Valid CreateOrderRequest req) {
        var response = orderService.create(user, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@CurrentUser User user, @PathVariable UUID orderId) {
        var response = orderService.getOrderById(user.getId(), orderId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin")
    public ResponseEntity<Page<OrderResponse>> getAll(
        @PageableDefault(page = 0, size = 20) Pageable pageable,
        @CurrentUser User user
    ) {
        var response = orderService.getAll(user, pageable);
        return ResponseEntity.ok(response);
    }

}