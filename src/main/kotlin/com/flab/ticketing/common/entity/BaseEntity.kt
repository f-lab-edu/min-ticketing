package com.flab.ticketing.common.entity

import jakarta.persistence.*
import java.time.ZonedDateTime
import java.util.*


@MappedSuperclass
abstract class BaseEntity {
    @Id
    @GeneratedValue
    var id: Long = 0L
        protected set

    @Column(updatable = false)
    var createdAt: ZonedDateTime = ZonedDateTime.now()

    var updatedAt: ZonedDateTime = ZonedDateTime.now()

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is BaseEntity) {
            return false
        }

        return other.id == this.id
    }

    override fun hashCode(): Int {
        return Objects.hash(id)
    }

    @PrePersist
    fun prePersist() {
        createdAt = ZonedDateTime.now()
        updatedAt = ZonedDateTime.now()
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = ZonedDateTime.now()
    }

}