package com.orderhub.service;

import java.math.BigDecimal;
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

        if (req.name() == null || req.name().isBlank()) {
            throw new AppException(ErrorCode.INVALID_INPUT, HttpStatus.BAD_REQUEST);
        }

        if (req.description() == null || req.description().isBlank()) {
            throw new AppException(ErrorCode.INVALID_INPUT, HttpStatus.BAD_REQUEST);
        }

        if (req.price() == null || req.price().compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.INVALID_INPUT, HttpStatus.BAD_REQUEST);
        }

        if (req.stock() == null || req.stock() <= 0) {
            throw new AppException(ErrorCode.INVALID_INPUT, HttpStatus.BAD_REQUEST);
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        }

        Jwt jwt = (Jwt) auth.getPrincipal();

        String ownerIdStr = jwt.getSubject();
        UUID ownerId = UUID.fromString(ownerIdStr);

        Optional<User> user = userRepository.findById(ownerId);
        if (user.isEmpty()) {
            throw new AppException(ErrorCode.USR_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        User owner = user.get();

        /**
         * Validação de segurança para validar se o userrealmente
         * tem a permissão correta para realizar aquela ação
        */
        if (!roleService.verifyRole(owner.getId(), "ADMIN")) {
            throw new AppException(ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        }

        /**
         * Valida se já não existe um produto com o mesmo
         * nome e que esteja atrelado ao usuário que está
         * realizando a request
        */
        if (productRepository.existsByNameAndOwnerId(req.name(), ownerId)) {
            throw new AppException(ErrorCode.DUPLICATED_RESOURCE, HttpStatus.CONFLICT);
        }

        Product product = new Product();
        product.setName(req.name());
        product.setDescription(req.description());
        product.setPrice(req.price());
        product.setStock(req.stock());
        product.setOwner(owner);

        Product saved = productRepository.save(product);
        return new CreatedResponse(saved.getId(), saved.getName(), saved.getDescription(), saved.getPrice(), true);
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

        if (pageable == null || !pageable.isPaged()) {
            throw new AppException(ErrorCode.INVALID_INPUT, HttpStatus.BAD_REQUEST);
        }

        Page<Product> products = productRepository.findAll(pageable);

        return products.map(product -> new ProductResponse(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getPrice()
        ));
    }    

}
