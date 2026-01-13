package com.orderhub.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import com.orderhub.entity.Product;

public interface ProductRepository extends JpaRepository<Product, UUID> {

}
