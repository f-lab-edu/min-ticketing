package com.flab.ticketing.performance.entity

import com.flab.ticketing.common.entity.BaseEntity
import com.flab.ticketing.common.entity.Region
import jakarta.persistence.*


@Entity
@Table(name = "performance_places")
class PerformancePlace(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private val region: Region,
    
    private val name: String,

    @OneToMany(mappedBy = "place", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    private val seats: List<PerformancePlaceSeat> = mutableListOf()
) : BaseEntity()