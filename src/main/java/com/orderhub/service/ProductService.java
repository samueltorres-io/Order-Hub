package com.orderhub.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.orderhub.dto.product.request.CreateRequest;
import com.orderhub.dto.product.request.UpdateRequest;
import com.orderhub.dto.product.response.CreatedResponse;
import com.orderhub.dto.product.response.ProductResponse;
import com.orderhub.entity.Product;
import com.orderhub.entity.User;
import com.orderhub.exception.AppException;
import com.orderhub.exception.ErrorCode;
import com.orderhub.repository.ProductRepository;
import com.orderhub.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final RoleService roleService;

    @Transactional
    public CreatedResponse create(CreateRequest req) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        }

        Jwt jwt = (Jwt) auth.getPrincipal();
        UUID ownerId = UUID.fromString(jwt.getSubject());

        User user = userRepository.findById(ownerId)
            .orElseThrow(() -> new AppException(ErrorCode.USR_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (!roleService.verifyRole(user.getId(), "ADMIN")) {
            throw new AppException(ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        }

        if (productRepository.existsByNameAndOwnerId(req.name(), ownerId)) {
            throw new AppException(ErrorCode.DUPLICATED_RESOURCE, HttpStatus.CONFLICT);
        }

        Product product = new Product();
        product.setName(req.name());
        product.setDescription(req.description());
        product.setPrice(req.price());
        product.setStock(req.stock());
        product.setOwner(user);

        Product saved = productRepository.save(product);
        return new CreatedResponse(saved.getId(), saved.getName(), saved.getDescription(), saved.getPrice(), product.getStatus());
    }

    @Transactional
    public CreatedResponse update(UpdateRequest req) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        }

        Jwt jwt = (Jwt) auth.getPrincipal();
        UUID userId = UUID.fromString(jwt.getSubject());

        if (!userRepository.existsById(userId)) {
            throw new AppException(ErrorCode.USR_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        if (!roleService.verifyRole(userId, "ADMIN")) {
            throw new AppException(ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        }

        Product product = productRepository.findById(req.id())
            .orElseThrow(() -> new AppException(
                ErrorCode.PRODUCT_NOT_FOUND,
                HttpStatus.NOT_FOUND
            ));

        if (!product.getOwner().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        }

        product.setName(req.name());
        product.setDescription(req.description());
        product.setPrice(req.price());
        product.setStatus(req.status());

        Product updatedProduct = productRepository.save(product);

        return new CreatedResponse(
            updatedProduct.getId(), 
            updatedProduct.getName(), 
            updatedProduct.getDescription(), 
            updatedProduct.getPrice(), 
            updatedProduct.getStatus()
        );
    }

    @Transactional
    public ProductResponse getByName(String name) {

        if (name == null || name.isBlank()) {
            throw new AppException(ErrorCode.INVALID_INPUT, HttpStatus.BAD_REQUEST);
        }

        Optional<Product> productExist = productRepository.findByName(name);
        if (productExist.isEmpty()) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        Product product = productExist.get();

        return new ProductResponse(product.getId(), product.getName(), product.getDescription(), product.getPrice());
    }

    public Page<ProductResponse> getProducts(Pageable pageable) {

        Page<Product> products = productRepository.findAll(pageable);

        return products.map(product -> new ProductResponse(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getPrice()
        ));
    }
}
