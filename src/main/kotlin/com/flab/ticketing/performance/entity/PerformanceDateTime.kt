package com.flab.ticketing.performance.entity

import com.flab.ticketing.common.entity.BaseEntity
import com.flab.ticketing.common.exception.BusinessIllegalStateException
import com.flab.ticketing.performance.exception.PerformanceErrorInfos
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

    ) : BaseEntity() {

    fun checkPassed(time: ZonedDateTime = ZonedDateTime.now()) {
        if (showTime.isBefore(time)) {
            throw BusinessIllegalStateException(PerformanceErrorInfos.PERFORMANCE_ALREADY_PASSED)
        }
    }


}