package com.orderhub.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface Order extends JpaRepository<Order, UUID> {

}
