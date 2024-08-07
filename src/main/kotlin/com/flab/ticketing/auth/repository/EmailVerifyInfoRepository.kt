package com.flab.ticketing.auth.repository

import com.flab.ticketing.auth.entity.EmailVerifyInfo
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface EmailVerifyInfoRepository : CrudRepository<EmailVerifyInfo, String>