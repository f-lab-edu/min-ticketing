package com.flab.ticketing.testutils

import com.flab.ticketing.common.entity.BaseEntity
import io.kotest.core.spec.style.StringSpec
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

abstract class UnitTest : StringSpec() {

    protected fun BaseEntity.setIdUsingReflection(newId: Long) {
        val property = BaseEntity::class.memberProperties.find { it.name == "id" }
        if (property != null && property is KMutableProperty<*>) {
            property.isAccessible = true
            property.setter.call(this, newId)
        } else {
            throw IllegalStateException("Cannot find mutable 'id' property in BaseEntity")
        }
    }

}