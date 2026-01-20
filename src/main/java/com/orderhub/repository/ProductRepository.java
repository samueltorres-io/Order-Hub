package com.orderhub.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.orderhub.entity.Product;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    boolean existsByNameAndOwnerId(String name, UUID ownerId);

    Optional<Product> findByName(String name);

}
