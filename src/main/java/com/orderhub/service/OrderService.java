package com.orderhub.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orderhub.dto.order.request.CreateOrderRequest;
import com.orderhub.dto.order.request.CreateOrderRequest.OrderItemRequest;
import com.orderhub.entity.Order;
import com.orderhub.entity.OrderItem;
import com.orderhub.entity.Product;
import com.orderhub.entity.User;
import com.orderhub.enums.OrderStatus;
import com.orderhub.exception.AppException;
import com.orderhub.exception.ErrorCode;
import com.orderhub.repository.ProductRepository;
import com.orderhub.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Transactional
    public void create(CreateOrderRequest req) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        }

        Jwt jwt = (Jwt) auth.getPrincipal();
        UUID userId = UUID.fromString(jwt.getSubject());

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USR_NOT_FOUND, HttpStatus.NOT_FOUND));

        List<UUID> productIds = req.items().stream().map(OrderItemRequest::productId).toList();
        List<Product> products = productRepository.findAllById(productIds);

        if (products.size() != productIds.size()) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.BAD_REQUEST); 
        }

        Map<UUID, Product> productMap = products.stream()
            .collect(Collectors.toMap(Product::getId, Function.identity()));

        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.pending);
        order.setCreatedAt(Instant.now());

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalOrderValue = BigDecimal.ZERO;

        for (OrderItemRequest itemReq : req.items()) {
            Product product = productMap.get(itemReq.productId());

            BigDecimal unitPrice = product.getPrice();
            BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.quantity()));

            totalOrderValue = totalOrderValue.add(itemTotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(itemReq.quantity());
            orderItem.setUnitPrice(unitPrice);

            orderItem.setOrder(order);
            orderItems.add(orderItem);
        }

        

    }

}
