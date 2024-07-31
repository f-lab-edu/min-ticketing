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


) {
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is User) {
            return false
        }

        return other.uid.equals(this.uid)

    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}
