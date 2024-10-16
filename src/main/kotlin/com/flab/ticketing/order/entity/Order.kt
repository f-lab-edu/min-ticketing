package com.flab.ticketing.order.entity

import com.flab.ticketing.common.entity.BaseEntity
import com.flab.ticketing.common.exception.BadRequestException
import com.flab.ticketing.order.exception.OrderErrorInfos
import com.flab.ticketing.performance.entity.PerformanceDateTime
import com.flab.ticketing.performance.entity.PerformancePlaceSeat
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

    @Embedded
    val payment: Payment,

    @Enumerated(EnumType.STRING)
    var status: OrderStatus = OrderStatus.REQUESTED

) : BaseEntity() {
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "order", cascade = [CascadeType.ALL])
    val reservations: MutableList<Reservation> = mutableListOf()

    var name: String = generateName()
    
    fun addReservation(reservation: Reservation) {
        reservations.add(reservation)
        name = generateName()
    }

    fun addReservation(performanceDateTime: PerformanceDateTime, seat: PerformancePlaceSeat) {
        val reservation = Reservation(performanceDateTime, seat, this)
        reservations.add(reservation)
        name = generateName()
    }


    private fun generateName(): String {
        if (reservations.size <= 0) {
            return ""
        }
        if (reservations.size == 1) {
            return reservations[0].performanceDateTime.performance.name + " 좌석 1건"
        }
        return reservations[0].performanceDateTime.performance.name + " 좌석 외 ${reservations.size - 1}건"

    }


    override fun prePersist() {
        super.prePersist()
        if (this.reservations.size == 0) {
            throw BadRequestException(OrderErrorInfos.ORDER_MUST_MINIMUM_ONE_RESERVATION)
        }
    }

    @Embeddable
    class Payment(
        val totalPrice: Int,
        val paymentMethod: String,
        var paymentKey: String? = null
    )


    enum class OrderStatus {
        REQUESTED, PENDING, COMPLETED, FAILED, CANCELED
    }
}