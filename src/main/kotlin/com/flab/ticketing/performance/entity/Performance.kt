package com.flab.ticketing.performance.entity

import com.flab.ticketing.common.entity.BaseEntity
import jakarta.persistence.*


@Entity
@Table(name = "performances")
class Performance(

    @Column(unique = true, updatable = false)
    private val uid: String,

    private val name: String,

    private val description: String,

    private val price: Int
) : BaseEntity() {

    @OneToMany(mappedBy = "performance", fetch = FetchType.LAZY)
    private lateinit var performanceDateTime: List<PerformanceDateTime>
}