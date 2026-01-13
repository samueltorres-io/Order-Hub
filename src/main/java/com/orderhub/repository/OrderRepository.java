package com.orderhub.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import com.orderhub.entity.Order;

public interface OrderRepository extends JpaRepository<Order, UUID> {

}