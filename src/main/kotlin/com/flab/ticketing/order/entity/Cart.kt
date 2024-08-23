package com.flab.ticketing.order.entity

import com.flab.ticketing.common.entity.BaseEntity
import com.flab.ticketing.user.entity.User
import jakarta.persistence.*


@Entity
@Table(
    name = "carts",
    uniqueConstraints = [UniqueConstraint(
        name = "reservate_unique",
        columnNames = ["seat_uid", "date_uid"]
    )]
)
class Cart(
    val seatUid: String,
    val dateUid: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User,
) : BaseEntity()