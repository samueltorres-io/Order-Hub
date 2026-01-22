package com.orderhub.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.orderhub.entity.Outbox;

public interface OutboxRepository extends JpaRepository<Outbox, UUID> {

}
