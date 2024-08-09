package com.flab.ticketing.auth.entity

import com.flab.ticketing.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "users")
class User(
    @Column(nullable = false, unique = true)
    val uid: String,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = true)
    var password: String,

    @Column(nullable = false)
    var nickname: String,

    ) : BaseEntity()
