package com.flab.ticketing.order.service

import com.flab.ticketing.auth.exception.AuthErrorInfos
import com.flab.ticketing.common.exception.DuplicatedException
import com.flab.ticketing.common.exception.InvalidValueException
import com.flab.ticketing.common.exception.NotFoundException
import com.flab.ticketing.common.exception.UnAuthorizedException
import com.flab.ticketing.order.entity.Cart
import com.flab.ticketing.order.exception.OrderErrorInfos
import com.flab.ticketing.order.repository.CartRepository
import com.flab.ticketing.order.repository.ReservationRepository
import com.flab.ticketing.performance.exception.PerformanceErrorInfos
import com.flab.ticketing.performance.repository.PerformanceDateRepository
import com.flab.ticketing.performance.repository.PerformanceRepository
import com.flab.ticketing.user.entity.repository.UserRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service


@Service
class ReservationService(
    private val userRepository: UserRepository,
    private val reservationRepository: ReservationRepository,
    private val performanceRepository: PerformanceRepository,
    private val performanceDateRepository: PerformanceDateRepository,
    private val cartRepository: CartRepository
) {

    fun reservate(
        userUid: String,
        performanceUid: String,
        dateUid: String,
        seatUid: String
    ) {
        val user = userRepository.findByUid(userUid) ?: throw UnAuthorizedException(AuthErrorInfos.USER_INFO_NOT_FOUND)

        val performance =
            performanceRepository.findPerformanceByUidJoinWithPlaceAndSeat(performanceUid)
                ?: throw NotFoundException(PerformanceErrorInfos.PERFORMANCE_NOT_FOUND)

        val performanceDateTime = performanceDateRepository.findByUid(dateUid)
            ?: throw NotFoundException(PerformanceErrorInfos.INVALID_PERFORMANCE_DATE)

        // performance에 performanceDate가 속했는지 검증
        if (performanceDateTime.performance != performance) {
            throw InvalidValueException(PerformanceErrorInfos.INVALID_PERFORMANCE_DATE)
        }

        // performancePlace에 seat가 속했는지 검증
        if (!performance.performancePlace.seats.map { it.uid }.contains(seatUid)) {
            throw InvalidValueException(PerformanceErrorInfos.PERFORMANCE_SEAT_INFO_INVALID)
        }

        // 예약이 존재하는지 확인
        if (reservationRepository.findReservationBySeatUidAndDateUid(seatUid, dateUid) != null) {
            throw DuplicatedException(OrderErrorInfos.ALREADY_RESERVATED)
        }

        try {
            cartRepository.save(Cart(seatUid, dateUid, user))
        } catch (e: DataIntegrityViolationException) {
            throw DuplicatedException(OrderErrorInfos.ALREADY_RESERVATED)
        }

    }

}