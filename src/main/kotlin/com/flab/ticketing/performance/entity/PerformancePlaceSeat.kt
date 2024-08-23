package com.flab.ticketing.performance.entity

import com.flab.ticketing.common.entity.BaseEntity
import jakarta.persistence.*


@Entity
@Table(name = "performance_place_seats")
class PerformancePlaceSeat(
    @Column(unique = true, updatable = false)
    val uid: String,

    val rowNum: Int,

    val columnNum: Int,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    val place: PerformancePlace
) : BaseEntity() {

    val name: String

    init {
        name = initializeName()
    }

    private fun initializeName(): String {
        val sb = StringBuilder()
        var n = rowNum
        while (n > 0) {
            n--
            val charCode = (n % 26 + 'A'.code).toChar()
            sb.insert(0, charCode)
            n /= 26
        }
        return sb.append(columnNum).toString()
    }
}