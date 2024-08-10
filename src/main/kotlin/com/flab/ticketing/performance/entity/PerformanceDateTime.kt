package com.flab.ticketing.performance.entity

import com.flab.ticketing.common.entity.BaseEntity
import jakarta.persistence.*
import java.time.ZonedDateTime


@Entity
@Table(name = "performance_datetimes")
class PerformanceDateTime(
    @Column(unique = true, updatable = false)
    private val uid: String,

    private val showTime: ZonedDateTime,
) : BaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id")
    private lateinit var performance: Performance

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private lateinit var performancePlace: PerformancePlace

}