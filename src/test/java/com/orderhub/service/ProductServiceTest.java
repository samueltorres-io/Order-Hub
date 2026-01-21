package com.orderhub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import com.orderhub.dto.product.request.CreateRequest;
import com.orderhub.dto.product.request.UpdateRequest;
import com.orderhub.dto.product.response.CreatedResponse;
import com.orderhub.dto.product.response.ProductResponse;
import com.orderhub.entity.Product;
import com.orderhub.entity.User;
import com.orderhub.enums.ProductStatus;
import com.orderhub.exception.AppException;
import com.orderhub.exception.ErrorCode;
import com.orderhub.repository.ProductRepository;
import com.orderhub.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleService roleService;

    @InjectMocks
    private ProductService productService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockSecurityContext(UUID userId) {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        Jwt jwt = mock(Jwt.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(userId.toString());

        SecurityContextHolder.setContext(securityContext);
    }

    @Nested
    @DisplayName("Tests for create()")
    class CreateTests {

        @Test
        @DisplayName("Should create product successfully when input is valid and user is ADMIN")
        void create_Success() {

            UUID userId = UUID.randomUUID();
            CreateRequest req = new CreateRequest("Iphone 15", "Smartphone", new BigDecimal("5000"), 10);
            
            mockSecurityContext(userId);
            
            User mockOwner = new User();
            mockOwner.setId(userId);

            Product mockSavedProduct = new Product();
            mockSavedProduct.setId(UUID.randomUUID());
            mockSavedProduct.setName(req.name());
            mockSavedProduct.setDescription(req.description());
            mockSavedProduct.setPrice(req.price());
            mockSavedProduct.setStatus(ProductStatus.active);

            when(userRepository.findById(userId)).thenReturn(Optional.of(mockOwner));
            when(roleService.verifyRole(userId, "ADMIN")).thenReturn(true);
            when(productRepository.existsByNameAndOwnerId(req.name(), userId)).thenReturn(false);
            when(productRepository.save(any(Product.class))).thenReturn(mockSavedProduct);

            CreatedResponse response = productService.create(req);

            assertThat(response).isNotNull();
            assertThat(response.name()).isEqualTo(req.name());
            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("Should throw exception if user is not ADMIN")
        void create_NotAdmin() {
            UUID userId = UUID.randomUUID();
            CreateRequest req = new CreateRequest("Item", "Desc", BigDecimal.TEN, 5);
            
            mockSecurityContext(userId);
            
            User user = new User();
            user.setId(userId); 
            
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(roleService.verifyRole(userId, "ADMIN")).thenReturn(false);

            assertThatThrownBy(() -> productService.create(req))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED)
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);

            verify(productRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception if product name is duplicated for owner")
        void create_Duplicated() {
            UUID userId = UUID.randomUUID();
            CreateRequest req = new CreateRequest("Item", "Desc", BigDecimal.TEN, 5);
            
            mockSecurityContext(userId);
            User user = new User();
            user.setId(userId);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(roleService.verifyRole(userId, "ADMIN")).thenReturn(true);
            when(productRepository.existsByNameAndOwnerId(req.name(), userId)).thenReturn(true);

            assertThatThrownBy(() -> productService.create(req))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATED_RESOURCE)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("Should throw exception for invalid input (Price <= 0)")
        void create_InvalidInput() {
            CreateRequest req = new CreateRequest("Item", "Desc", BigDecimal.ZERO, 5);

            assertThatThrownBy(() -> productService.create(req))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("Tests for update()")
    class UpdateTests {

        @Test
        @DisplayName("Should update product successfully")
        void update_Success() {
            UUID userId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            UpdateRequest req = new UpdateRequest(productId, "New Name", "New Desc", new BigDecimal("100"), 0, ProductStatus.active);

            mockSecurityContext(userId);

            User owner = new User();
            owner.setId(userId);

            Product existingProduct = new Product();
            existingProduct.setId(productId);
            existingProduct.setOwner(owner);

            when(userRepository.existsById(userId)).thenReturn(true);
            when(roleService.verifyRole(userId, "ADMIN")).thenReturn(true);
            when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
            when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

            CreatedResponse response = productService.update(req);

            assertThat(response.name()).isEqualTo("New Name");
            assertThat(response.description()).isEqualTo("New Desc");
            verify(productRepository).save(existingProduct);
        }

        @Test
        @DisplayName("Should throw exception if user tries to update product that is not theirs")
        void update_NotOwner() {
            UUID userId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            UpdateRequest req = new UpdateRequest(productId, "Name", "Desc", BigDecimal.TEN, 0, ProductStatus.active);

            mockSecurityContext(userId);

            User otherOwner = new User();
            otherOwner.setId(otherUserId);

            Product product = new Product();
            product.setId(productId);
            product.setOwner(otherOwner);

            when(userRepository.existsById(userId)).thenReturn(true);
            when(roleService.verifyRole(userId, "ADMIN")).thenReturn(true);
            when(productRepository.findById(productId)).thenReturn(Optional.of(product));

            assertThatThrownBy(() -> productService.update(req))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED)
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("Tests for getByName()")
    class GetByNameTests {

        @Test
        @DisplayName("Should return product response")
        void getByName_Success() {
            String name = "Test Product";
            Product product = new Product();
            product.setId(UUID.randomUUID());
            product.setName(name);
            product.setDescription("Desc");
            product.setPrice(BigDecimal.TEN);

            when(productRepository.findByName(name)).thenReturn(Optional.of(product));

            ProductResponse response = productService.getByName(name);

            assertThat(response.name()).isEqualTo(name);
        }

        @Test
        @DisplayName("Should throw exception when product not found")
        void getByName_NotFound() {
            String name = "Unknown";
            when(productRepository.findByName(name)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getByName(name))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("Tests for getProducts() [Pagination]")
    class PaginationTests {

        @Test
        @DisplayName("Should return paginated products")
        void getProducts_Success() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            
            Product p1 = new Product();
            p1.setId(UUID.randomUUID());
            p1.setName("P1");
            p1.setPrice(BigDecimal.TEN);

            Page<Product> productPage = new PageImpl<>(List.of(p1));

            when(productRepository.findAll(pageable)).thenReturn(productPage);

            // Act
            Page<ProductResponse> result = productService.getProducts(pageable);

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.getContent().get(0).name()).isEqualTo("P1");
        }

        @Test
        @DisplayName("Should throw exception if pageable is invalid/null")
        void getProducts_InvalidPageable() {

            assertThatThrownBy(() -> productService.getProducts(Pageable.unpaged()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
        }
    }
}