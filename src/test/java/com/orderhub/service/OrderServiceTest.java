package com.orderhub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderhub.dto.order.request.CreateOrderRequest;
import com.orderhub.dto.order.request.CreateOrderRequest.OrderItemRequest;
import com.orderhub.dto.order.response.OrderResponse;
import com.orderhub.entity.Order;
import com.orderhub.entity.Outbox;
import com.orderhub.entity.Product;
import com.orderhub.entity.User;
import com.orderhub.enums.OrderStatus;
import com.orderhub.exception.AppException;
import com.orderhub.exception.ErrorCode;
import com.orderhub.repository.OrderRepository;
import com.orderhub.repository.OutboxRepository;
import com.orderhub.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OutboxRepository outboxRepository;
    @Mock private RoleService roleService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks private OrderService orderService;

    @Nested
    @DisplayName("Tests for create()")
    class CreateTests {

        @Test
        @DisplayName("Should create order successfully")
        void create_Success() throws JsonProcessingException {
            // Arrange
            User user = new User();
            user.setId(UUID.randomUUID());

            UUID productId = UUID.randomUUID();
            CreateOrderRequest req = new CreateOrderRequest(List.of(
                new OrderItemRequest(productId, 2)
            ));

            Product product = new Product();
            product.setId(productId);
            product.setPrice(new BigDecimal("100.00"));
            product.setName("Test Product");

            when(productRepository.findAllById(anyList())).thenReturn(List.of(product));
            
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                order.setId(UUID.randomUUID());
                order.getItems().forEach(item -> item.setId(UUID.randomUUID()));
                return order;
            });

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            OrderResponse response = orderService.create(user, req);

            assertThat(response).isNotNull();
            assertThat(response.total()).isEqualTo(new BigDecimal("200.00"));
            assertThat(response.status()).isEqualTo(OrderStatus.pending);
            
            verify(orderRepository).save(any(Order.class));
            verify(outboxRepository).save(any(Outbox.class));
        }

        @Test
        @DisplayName("Should throw exception when product count mismatch (Product not found)")
        void create_ProductNotFound() {
            User user = new User();
            UUID productId = UUID.randomUUID();
            CreateOrderRequest req = new CreateOrderRequest(List.of(
                new OrderItemRequest(productId, 1)
            ));

            when(productRepository.findAllById(anyList())).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> orderService.create(user, req))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INTERNAL_SERVER_ERROR)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("Tests for getOrderById()")
    class GetByIdTests {

        @Test
        @DisplayName("Should return order when user is owner")
        void getById_Success_Owner() {
            UUID userId = UUID.randomUUID();
            User user = new User();
            user.setId(userId);

            UUID orderId = UUID.randomUUID();
            Order order = new Order();
            order.setId(orderId);
            order.setUser(user);
            order.setTotal(BigDecimal.TEN);
            order.setItems(List.of());

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            OrderResponse response = orderService.getOrderById(userId, orderId);

            assertThat(response.orderId()).isEqualTo(orderId);
        }

        @Test
        @DisplayName("Should throw exception when user is NOT owner and NOT admin")
        void getById_Unauthorized() {
            UUID userId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            
            User owner = new User();
            owner.setId(ownerId);

            UUID orderId = UUID.randomUUID();
            Order order = new Order();
            order.setId(orderId);
            order.setUser(owner);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(roleService.verifyRole(userId, "ADMIN")).thenReturn(false);

            assertThatThrownBy(() -> orderService.getOrderById(userId, orderId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
        }
    }
}