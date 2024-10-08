package com.flab.ticketing.user.repository

import com.flab.ticketing.user.entity.User
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : CrudRepository<User, Long> {
    fun findByEmail(email: String): User?

    fun findByUid(uid: String): User?
}