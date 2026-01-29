package com.orderhub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

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

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private RoleService roleService;

    @InjectMocks
    private ProductService productService;

    @Nested
    @DisplayName("Tests for create()")
    class CreateTests {

        @Test
        @DisplayName("Should create product successfully when input is valid and user is ADMIN")
        void create_Success() {
            // Arrange
            UUID userId = UUID.randomUUID();
            User user = new User();
            user.setId(userId);

            CreateRequest req = new CreateRequest("Iphone 15", "Smartphone", new BigDecimal("5000"), 10);
            
            Product mockSavedProduct = new Product();
            mockSavedProduct.setId(UUID.randomUUID());
            mockSavedProduct.setName(req.name());
            mockSavedProduct.setDescription(req.description());
            mockSavedProduct.setPrice(req.price());
            mockSavedProduct.setStatus(ProductStatus.active);
            mockSavedProduct.setOwner(user);

            when(roleService.verifyRole(userId, "ADMIN")).thenReturn(true);
            when(productRepository.existsByNameAndOwnerId(req.name(), userId)).thenReturn(false);
            when(productRepository.save(any(Product.class))).thenReturn(mockSavedProduct);

            // Act
            CreatedResponse response = productService.create(user, req);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.name()).isEqualTo(req.name());
            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("Should throw exception if user is not ADMIN")
        void create_NotAdmin() {
            // Arrange
            UUID userId = UUID.randomUUID();
            User user = new User();
            user.setId(userId);

            CreateRequest req = new CreateRequest("Item", "Desc", BigDecimal.TEN, 5);
            
            when(roleService.verifyRole(userId, "ADMIN")).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> productService.create(user, req))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED)
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);

            verify(productRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception if product name is duplicated for owner")
        void create_Duplicated() {
            // Arrange
            UUID userId = UUID.randomUUID();
            User user = new User();
            user.setId(userId);

            CreateRequest req = new CreateRequest("Item", "Desc", BigDecimal.TEN, 5);
            
            when(roleService.verifyRole(userId, "ADMIN")).thenReturn(true);
            when(productRepository.existsByNameAndOwnerId(req.name(), userId)).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> productService.create(user, req))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATED_RESOURCE)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);
            
            verify(productRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tests for update()")
    class UpdateTests {

        @Test
        @DisplayName("Should update product successfully")
        void update_Success() {
            // Arrange
            UUID userId = UUID.randomUUID();
            User user = new User();
            user.setId(userId);

            UUID productId = UUID.randomUUID();
            UpdateRequest req = new UpdateRequest(productId, "New Name", "New Desc", new BigDecimal("100"), 0, ProductStatus.active);

            Product existingProduct = new Product();
            existingProduct.setId(productId);
            existingProduct.setOwner(user); // Dono correto

            when(roleService.verifyRole(userId, "ADMIN")).thenReturn(true);
            when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
            when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            CreatedResponse response = productService.update(user, req);

            // Assert
            assertThat(response.name()).isEqualTo("New Name");
            assertThat(response.description()).isEqualTo("New Desc");
            verify(productRepository).save(existingProduct);
        }

        @Test
        @DisplayName("Should throw exception if user tries to update product that is not theirs")
        void update_NotOwner() {
            // Arrange
            UUID userId = UUID.randomUUID();
            User user = new User();
            user.setId(userId);

            UUID otherUserId = UUID.randomUUID();
            User otherOwner = new User();
            otherOwner.setId(otherUserId);

            UUID productId = UUID.randomUUID();
            UpdateRequest req = new UpdateRequest(productId, "Name", "Desc", BigDecimal.TEN, 0, ProductStatus.active);

            Product product = new Product();
            product.setId(productId);
            product.setOwner(otherOwner); // Dono diferente

            when(roleService.verifyRole(userId, "ADMIN")).thenReturn(true);
            when(productRepository.findById(productId)).thenReturn(Optional.of(product));

            // Act & Assert
            assertThatThrownBy(() -> productService.update(user, req))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED)
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Should throw exception if product not found")
        void update_ProductNotFound() {
            // Arrange
            UUID userId = UUID.randomUUID();
            User user = new User();
            user.setId(userId);
            
            UUID productId = UUID.randomUUID();
            UpdateRequest req = new UpdateRequest(productId, "Name", "Desc", BigDecimal.TEN, 0, ProductStatus.active);

            when(roleService.verifyRole(userId, "ADMIN")).thenReturn(true);
            when(productRepository.findById(productId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> productService.update(user, req))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
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
        
        // O teste de "Invalid Pageable" foi removido pois o Service apenas repassa o objeto.
        // A validação de nulo ou unpaged geralmente ocorre no Controller ou lança NPE se o repositório não suportar.
        // Se quiser testar null, o repositório provavelmente lançaria exceção, mas não é lógica de negócio do service.
    }
}