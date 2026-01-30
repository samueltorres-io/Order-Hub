package com.orderhub.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderhub.dto.order.request.CreateOrderRequest;
import com.orderhub.dto.order.request.CreateOrderRequest.OrderItemRequest;
import com.orderhub.dto.order.response.OrderResponse;
import com.orderhub.entity.Order;
import com.orderhub.entity.OrderItem;
import com.orderhub.entity.Outbox;
import com.orderhub.entity.Product;
import com.orderhub.entity.User;
import com.orderhub.enums.OrderStatus;
import com.orderhub.enums.OutboxStatus;
import com.orderhub.exception.AppException;
import com.orderhub.exception.ErrorCode;
import com.orderhub.repository.OrderRepository;
import com.orderhub.repository.OutboxRepository;
import com.orderhub.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final RoleService roleService;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrderResponse create(User user, CreateOrderRequest req) {

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
            orderItem.setSubtotal(itemTotal);
            orderItem.setOrder(order);
            orderItems.add(orderItem);
        }

        if (totalOrderValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.BAD_REQUEST);
        }

        order.setTotal(totalOrderValue);

        order.setItems(orderItems); 

        Order savedOrder = orderRepository.save(order);

        try {
            String orderJson = objectMapper.writeValueAsString(savedOrder);

            Outbox outbox = Outbox.builder()
                .topic("orders-events")
                .aggregateId(savedOrder.getId().toString())
                .eventType("ORDER_CREATED")
                .payload(orderJson)
                .status(OutboxStatus.PENDING)
                .createdAt(Instant.now())
                .build();

            outboxRepository.save(outbox);

        } catch (Exception e) {
            e.printStackTrace(); 
            throw new RuntimeException("Error processing outbox event", e);
        }

        List<OrderResponse.OrderItemResponse> itemResponse = savedOrder.getItems().stream()
            .map(item -> new OrderResponse.OrderItemResponse(
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
            ))
            .toList();

        return new OrderResponse(
            savedOrder.getId(),
            savedOrder.getTotal(),
            savedOrder.getStatus(),
            savedOrder.getCreatedAt(),
            itemResponse
        );
    }

    public Page<OrderResponse> getAll(User user, Pageable pageable) {

        if (!roleService.verifyRole(user.getId(), "ADMIN")) {
            throw new AppException(ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        }

        return orderRepository.findAll(pageable)
            .map(this::mapToOrderResponse);
    }

    public OrderResponse getOrderById(UUID userId, UUID orderId) {

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND, HttpStatus.NOT_FOUND));

        /* User logado e dono da order ou Admin */
        if (
            order.getUser().getId() != userId &&
            !roleService.verifyRole(userId, "ADMIN")
        ) { throw new AppException(ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED); }
            
        return mapToOrderResponse(order);
    }

    private OrderResponse mapToOrderResponse(Order order) {
        List<OrderResponse.OrderItemResponse> items = order.getItems().stream()
            .map(item -> new OrderResponse.OrderItemResponse(
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
            ))
            .toList();
    
        return new OrderResponse(
            order.getId(),
            order.getTotal(),
            order.getStatus(),
            order.getCreatedAt(),
            items
        );
    }
}