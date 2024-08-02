package com.flab.ticketing.common.entity

import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import java.util.*


@MappedSuperclass
abstract class BaseEntity {
    @Id
    @GeneratedValue
    var id: Long = 0L
        protected set


    override fun equals(other: Any?): Boolean {
        if (other == null || other !is BaseEntity) {
            return false
        }

        return other.id == this.id
    }

    override fun hashCode(): Int {
        return Objects.hash(id)
    }

}