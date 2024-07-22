package com.flab.ticketing.user.repository

interface EmailRepository {

    fun saveCode(email : String, code : String)
}