package com.flab.ticketing.order.entity

import com.flab.ticketing.common.entity.BaseEntity
import com.flab.ticketing.user.entity.User
import jakarta.persistence.*

@Entity
@Table(name = "orders")
class Order(
    @Column(unique = true, updatable = false)
    val uid: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordered_user_id")
    val user: User,

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "order")
    val reservations: MutableList<Reservation> = mutableListOf(),

    @Embedded
    val payment: Payment

) : BaseEntity() {

    fun addReservation(reservation: Reservation) {
        reservations.add(reservation)
    }


    @Embeddable
    class Payment(
        val totalPrice: Int,
        val paymentMethod: String
    )

}