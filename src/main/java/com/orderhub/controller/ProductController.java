package com.orderhub.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orderhub.dto.product.request.CreateRequest;
import com.orderhub.dto.product.response.CreatedResponse;
import com.orderhub.dto.product.response.ProductResponse;
import com.orderhub.service.ProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @PostMapping("/")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    public ResponseEntity<CreatedResponse> create(@Valid @RequestBody CreateRequest req) {
        var response = productService.create(req);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{name}")
    public ResponseEntity<ProductResponse> getByName(@PathVariable String name) {
        var response = productService.getByName(name);
        return ResponseEntity.ok(response);
    }

}
