package com.flab.ticketing.performance.entity

import com.flab.ticketing.common.entity.BaseEntity
import jakarta.persistence.*


@Entity
@Table(name = "performance_places")
class PerformancePlace(
    private val name: String,

    @OneToMany(mappedBy = "place", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    private val seats: List<PerformancePlaceSeat> = mutableListOf()
) : BaseEntity()