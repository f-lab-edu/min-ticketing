package com.flab.ticketing.user.entity

import jakarta.persistence.*

@Entity
@Table(name = "users")
data class User(
    @Column(nullable = false, unique = true)
    val uid: String,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = true)
    val password: String,

    @Column(nullable = false)
    val nickname: String,

    @Id
    @GeneratedValue
    val id: Long? = null
)
