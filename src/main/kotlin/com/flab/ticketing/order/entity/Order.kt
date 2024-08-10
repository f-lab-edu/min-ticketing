package com.flab.ticketing.order.entity

import com.flab.ticketing.auth.entity.User
import com.flab.ticketing.common.entity.BaseEntity
import jakarta.persistence.*

@Entity
class Order(
    @Column(unique = true, updatable = false)
    private val uid: String,

    @Embedded
    val payment: Payment
) : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordered_user_id")
    private lateinit var user: User

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "order")
    private lateinit var reservations: List<Reservation>

    @Embeddable
    class Payment(
        val totalPrice: Int,
        val paymentMethod: String
    )

}