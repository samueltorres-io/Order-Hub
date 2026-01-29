package com.orderhub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderhub.dto.order.request.CreateOrderRequest;
import com.orderhub.dto.order.response.OrderResponse;
import com.orderhub.entity.Order;
import com.orderhub.entity.OrderItem;
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

    @Mock
    private ProductRepository productRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OutboxRepository outboxRepository;
    @Mock
    private RoleService roleService;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderService orderService;

    @Nested
    @DisplayName("Tests for create()")
    class CreateTests {

        @Test
        @DisplayName("Should create order and outbox event successfully")
        void create_Success() throws JsonProcessingException {
            User user = new User();
            user.setId(UUID.randomUUID());

            UUID productId = UUID.randomUUID();
            Product product = new Product();
            product.setId(productId);
            product.setPrice(BigDecimal.TEN);
            product.setName("Test Product");

            CreateOrderRequest.OrderItemRequest itemReq = new CreateOrderRequest.OrderItemRequest(productId, 2);
            CreateOrderRequest request = new CreateOrderRequest(List.of(itemReq));

            when(productRepository.findAllById(anyList())).thenReturn(List.of(product));
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"json\":\"order\"}");
            
            // Mock do save retornando a order com ID
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
                Order order = inv.getArgument(0);
                order.setId(UUID.randomUUID());
                return order;
            });

            OrderResponse response = orderService.create(user, request);

            assertThat(response).isNotNull();
            assertThat(response.total()).isEqualTo(BigDecimal.valueOf(20)); // 10 * 2
            assertThat(response.status()).isEqualTo(OrderStatus.pending);

            verify(orderRepository).save(any(Order.class));
            verify(outboxRepository).save(any(Outbox.class));
        }

        @Test
        @DisplayName("Should throw exception when product count mismatches")
        void create_ProductNotFound() {
            User user = new User();
            UUID productId = UUID.randomUUID();
            CreateOrderRequest request = new CreateOrderRequest(
                List.of(new CreateOrderRequest.OrderItemRequest(productId, 1))
            );

            // Simula produto não encontrado (lista vazia)
            when(productRepository.findAllById(anyList())).thenReturn(List.of());

            assertThatThrownBy(() -> orderService.create(user, request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INTERNAL_SERVER_ERROR)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);

            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw RuntimeException when Outbox JSON processing fails")
        void create_OutboxJsonError() throws JsonProcessingException {
            User user = new User();
            UUID productId = UUID.randomUUID();
            Product product = new Product();
            product.setId(productId);
            product.setPrice(BigDecimal.TEN);
            
            CreateOrderRequest request = new CreateOrderRequest(
                List.of(new CreateOrderRequest.OrderItemRequest(productId, 1))
            );

            when(productRepository.findAllById(anyList())).thenReturn(List.of(product));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
                Order o = inv.getArgument(0);
                o.setId(UUID.randomUUID());
                return o;
            });

            // Simula erro no Jackson
            when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Error") {});

            assertThatThrownBy(() -> orderService.create(user, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error processing outbox event");

            // Confirma que a ordem foi salva, mas o outbox não
            verify(orderRepository).save(any(Order.class));
            verify(outboxRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tests for getAll()")
    class GetAllTests {

        @Test
        @DisplayName("Should return page of orders when user is ADMIN")
        void getAll_Success() {
            User adminUser = new User();
            adminUser.setId(UUID.randomUUID());
            Pageable pageable = Pageable.unpaged();

            when(roleService.verifyRole(adminUser.getId(), "ADMIN")).thenReturn(true);
            when(orderRepository.findAll(pageable)).thenReturn(Page.empty());

            Page<OrderResponse> result = orderService.getAll(adminUser, pageable);

            assertThat(result).isNotNull();
            verify(orderRepository).findAll(pageable);
        }

        @Test
        @DisplayName("Should throw UNAUTHORIZED when user is not ADMIN")
        void getAll_Unauthorized() {
            User user = new User();
            user.setId(UUID.randomUUID());
            Pageable pageable = Pageable.unpaged();

            when(roleService.verifyRole(user.getId(), "ADMIN")).thenReturn(false);

            assertThatThrownBy(() -> orderService.getAll(user, pageable))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED)
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);

            verify(orderRepository, never()).findAll(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("Tests for getOrderById()")
    class GetOrderByIdTests {

        @Test
        @DisplayName("Should return order when user is the Owner")
        void getOrderById_Success_Owner() {
            UUID userId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();

            User owner = new User();
            owner.setId(userId);

            Order order = createMockOrder(orderId, owner);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            
            // Não precisamos mockar o RoleService aqui devido ao Short-Circuit do Java.
            // (order.getUser().getId() != userId) é FALSE.
            // FALSE && (qualquer coisa) = FALSE. O if é pulado.

            OrderResponse response = orderService.getOrderById(userId, orderId);

            assertThat(response).isNotNull();
            assertThat(response.orderId()).isEqualTo(orderId);
            verify(roleService, never()).verifyRole(any(), any());
        }

        @Test
        @DisplayName("Should return order when user is NOT Owner but is ADMIN")
        void getOrderById_Success_Admin() {
            UUID userId = UUID.randomUUID(); // ID do admin logado
            UUID orderId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID(); // ID do dono do pedido

            User owner = new User();
            owner.setId(ownerId);

            Order order = createMockOrder(orderId, owner);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            // Não é dono, então verifica a role
            when(roleService.verifyRole(userId, "ADMIN")).thenReturn(true);

            OrderResponse response = orderService.getOrderById(userId, orderId);

            assertThat(response).isNotNull();
            assertThat(response.orderId()).isEqualTo(orderId);
        }

        @Test
        @DisplayName("Should throw UNAUTHORIZED when user is neither Owner nor Admin")
        void getOrderById_Unauthorized() {
            UUID userId = UUID.randomUUID(); // Hacker tentando acessar
            UUID orderId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID(); // Dono real

            User owner = new User();
            owner.setId(ownerId);

            Order order = new Order();
            order.setUser(owner);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            // Não é dono E não é admin
            when(roleService.verifyRole(userId, "ADMIN")).thenReturn(false);

            assertThatThrownBy(() -> orderService.getOrderById(userId, orderId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED)
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Should throw NOT_FOUND when order ID does not exist")
        void getOrderById_NotFound() {
            UUID userId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();

            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOrderById(userId, orderId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
        }
        
        // Helper method para criar uma Order válida com items
        private Order createMockOrder(UUID orderId, User owner) {
            Order order = new Order();
            order.setId(orderId);
            order.setUser(owner);
            order.setTotal(BigDecimal.TEN);
            order.setStatus(OrderStatus.pending);
            
            Product p = new Product();
            p.setId(UUID.randomUUID());
            p.setPrice(BigDecimal.TEN);
            
            OrderItem item = new OrderItem();
            item.setProduct(p);
            item.setQuantity(1);
            item.setUnitPrice(BigDecimal.TEN);
            order.setItems(List.of(item));
            
            return order;
        }
    }
}