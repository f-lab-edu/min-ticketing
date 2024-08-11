package com.flab.ticketing.performance.entity

import com.flab.ticketing.common.entity.BaseEntity
import jakarta.persistence.*


@Entity
@Table(name = "performances")
class Performance(

    @Column(unique = true, updatable = false)
    private val uid: String,

    private val name: String,

    private val image: String,

    private val description: String,

    private val price: Int,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private val performancePlace: PerformancePlace,

    @OneToMany(mappedBy = "performance", fetch = FetchType.LAZY)
    private val performanceDateTime: List<PerformanceDateTime> = mutableListOf()
) : BaseEntity()