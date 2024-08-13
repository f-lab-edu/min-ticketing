package com.flab.ticketing.performance.entity

import com.flab.ticketing.common.entity.BaseEntity
import jakarta.persistence.*
import java.time.ZonedDateTime


@Entity
@Table(name = "performances")
class Performance(

    @Column(unique = true, updatable = false)
    val uid: String,

    val name: String,

    val image: String,

    val description: String,

    val price: Int,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    val performancePlace: PerformancePlace,

    @OneToMany(mappedBy = "performance", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    val performanceDateTime: MutableList<PerformanceDateTime> = mutableListOf()
) : BaseEntity() {

    fun addDateTime(
        uid: String,
        showTime: ZonedDateTime,

        ) {
        val dateTime = PerformanceDateTime(uid, showTime, this)
        performanceDateTime.add(dateTime)
    }

}