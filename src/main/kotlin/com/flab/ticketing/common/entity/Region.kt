package com.flab.ticketing.common.entity

import jakarta.persistence.Entity
import jakarta.persistence.Table


@Entity
@Table(name = "regions")
class Region(
    val uid: String,
    val name: String
) : BaseEntity()