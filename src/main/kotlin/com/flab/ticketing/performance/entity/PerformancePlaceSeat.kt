package com.flab.ticketing.performance.entity

import com.flab.ticketing.common.entity.BaseEntity
import jakarta.persistence.*


@Entity
@Table(name = "performance_place_seats")
class PerformancePlaceSeat(
    @Column(unique = true, updatable = false)
    private val uid: String,

    private val row: Int,

    private val column: Int
) : BaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private lateinit var place: PerformancePlace

    @Transient
    lateinit var name: String
        private set

    init {
        initializeName()
    }

    private fun initializeName() {
        val sb = StringBuilder()
        var n = row
        while (n > 0) {
            n--
            val charCode = (n % 26 + 'A'.code).toChar()
            sb.insert(0, charCode)
            n /= 26
        }
        name = sb.append(column).toString()
    }
}