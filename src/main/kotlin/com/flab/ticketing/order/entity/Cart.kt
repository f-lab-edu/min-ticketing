package com.flab.ticketing.order.entity

import com.flab.ticketing.common.entity.BaseEntity
import com.flab.ticketing.performance.entity.PerformanceDateTime
import com.flab.ticketing.performance.entity.PerformancePlaceSeat
import com.flab.ticketing.user.entity.User
import jakarta.persistence.*


@Entity
@Table(
    name = "carts",
    uniqueConstraints = [UniqueConstraint(
        name = "ux_seat_uid_date_uid",
        columnNames = ["seat_id", "performance_datetime_id"]
    )]
)
class Cart(

    @Column(unique = true, updatable = false)
    val uid: String,

    @ManyToOne
    @JoinColumn(name = "seat_id")
    val seat: PerformancePlaceSeat,


    @ManyToOne
    @JoinColumn(name = "performance_datetime_id")
    val performanceDateTime: PerformanceDateTime,

    @ManyToOne
    @JoinColumn(name = "user_id")
    val user: User,
) : BaseEntity()