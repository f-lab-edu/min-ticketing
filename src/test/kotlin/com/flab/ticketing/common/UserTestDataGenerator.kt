package com.flab.ticketing.common

import com.flab.ticketing.user.entity.User

object UserTestDataGenerator {

    fun createUser(
        uid: String = "uid1232",
        email: String = "email@email.com",
        encryptedPassword: String = "enc123Rypt42ed",
        nickname: String = "nickname"
    ): User{
        return User(
            uid,
            email,
            encryptedPassword,
            nickname
        )
    }

}