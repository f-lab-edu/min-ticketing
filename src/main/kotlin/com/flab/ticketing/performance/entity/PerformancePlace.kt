package com.flab.ticketing.performance.entity

import com.flab.ticketing.common.entity.BaseEntity
import com.flab.ticketing.common.entity.Region
import com.flab.ticketing.common.exception.InvalidValueException
import com.flab.ticketing.performance.exception.PerformanceErrorInfos
import jakarta.persistence.*


@Entity
@Table(name = "performance_places")
class PerformancePlace(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    val region: Region,

    val name: String,

    @OneToMany(mappedBy = "place", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    val seats: MutableList<PerformancePlaceSeat> = mutableListOf()
) : BaseEntity() {

    fun addSeat(
        uid: String,
        row: Int,
        column: Int
    ) {
        val seat = PerformancePlaceSeat(uid, row, column, this)
        this.seats.add(seat)
    }

    fun findSeatIn(seatUid: String): PerformancePlaceSeat {
        val seat = seats.find { it.uid == seatUid }
            ?: throw InvalidValueException(PerformanceErrorInfos.PERFORMANCE_SEAT_INFO_INVALID)

        return seat
    }

}