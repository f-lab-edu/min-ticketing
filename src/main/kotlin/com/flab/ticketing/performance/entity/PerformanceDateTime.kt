package com.flab.ticketing.performance.entity

import com.flab.ticketing.common.entity.BaseEntity
import jakarta.persistence.*
import java.time.ZonedDateTime


@Entity
@Table(name = "performance_datetimes")
class PerformanceDateTime(
    @Column(unique = true, updatable = false)
    val uid: String,

    val showTime: ZonedDateTime,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id")
    val performance: Performance,

    ) : BaseEntity()