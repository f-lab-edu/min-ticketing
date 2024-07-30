package com.flab.ticketing.user.repository

import com.flab.ticketing.user.entity.EmailVerifyInfo
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface EmailVerifyInfoRepository : CrudRepository<EmailVerifyInfo, String>