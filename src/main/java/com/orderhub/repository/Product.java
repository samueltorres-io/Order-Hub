package com.orderhub.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface Product extends JpaRepository<Product, UUID> {

}
