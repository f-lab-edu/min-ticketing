package com.flab.ticketing.order.entity

import com.flab.ticketing.auth.entity.User
import com.flab.ticketing.common.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "orders")
class Order(
    @Column(unique = true, updatable = false)
    private val uid: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordered_user_id")
    private val user: User,

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "order")
    private val reservations: List<Reservation> = mutableListOf(),

    @Embedded
    val payment: Payment

) : BaseEntity() {


    @Embeddable
    class Payment(
        val totalPrice: Int,
        val paymentMethod: String
    )

}