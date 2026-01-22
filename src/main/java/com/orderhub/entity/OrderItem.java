package com.orderhub.entity;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.Check;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.generator.EventType;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "order_items", uniqueConstraints = {
    @UniqueConstraint(name = "unique_order_product", columnNames = { "order_id", "product_id" })
})
public class OrderItem {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private Order order;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    @Check(constraints = "quantity > 0")
    @Min(1)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    @Check(constraints = "unit_price >= 0")
    @PositiveOrZero
    private BigDecimal unitPrice;

    @Column(insertable = false, updatable = false)
    @Generated(event = { EventType.INSERT, EventType.UPDATE })
    private BigDecimal subtotal;

}