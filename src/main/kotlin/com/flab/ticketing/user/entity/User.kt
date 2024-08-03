package com.flab.ticketing.user.entity

import com.flab.ticketing.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "users")
class User(
    private val _uid: String,
    private val _email: String,
    private val _password: String,
    private val _nickname: String
) : BaseEntity() {

    @Column(nullable = false, unique = true)
    val uid: String = _uid

    @Column(nullable = false, unique = true)
    val email: String = _email
    
    @Column(nullable = true)
    var password: String = _password
        protected set

    @Column(nullable = false)
    var nickname: String = _nickname
        protected set

}
