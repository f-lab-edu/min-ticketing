package com.flab.ticketing.order.service

import com.flab.ticketing.auth.exception.AuthErrorInfos
import com.flab.ticketing.common.OrderTestDataGenerator
import com.flab.ticketing.common.PerformanceTestDataGenerator
import com.flab.ticketing.common.UnitTest
import com.flab.ticketing.common.UserTestDataGenerator
import com.flab.ticketing.common.exception.DuplicatedException
import com.flab.ticketing.common.exception.InvalidValueException
import com.flab.ticketing.common.exception.NotFoundException
import com.flab.ticketing.common.exception.UnAuthorizedException
import com.flab.ticketing.order.entity.Cart
import com.flab.ticketing.order.entity.Reservation
import com.flab.ticketing.order.exception.OrderErrorInfos
import com.flab.ticketing.order.repository.CartRepository
import com.flab.ticketing.order.repository.ReservationRepository
import com.flab.ticketing.performance.exception.PerformanceErrorInfos
import com.flab.ticketing.performance.repository.PerformanceDateRepository
import com.flab.ticketing.performance.repository.PerformanceRepository
import com.flab.ticketing.user.service.reader.UserReader
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.dao.DataIntegrityViolationException

class ReservationServiceTest : UnitTest() {
    private val performanceRepository: PerformanceRepository = mockk()
    private val performanceDateRepository: PerformanceDateRepository = mockk()
    private val userReader: UserReader = mockk()
    private val cartRepository: CartRepository = mockk()
    private val reservationRepository: ReservationRepository = mockk()

    private val reservationService =
        ReservationService(
            userReader,
            reservationRepository,
            performanceRepository,
            performanceDateRepository,
            cartRepository
        )

    init {
        "예약 정보를 저장할 수 있다." {
            val user = UserTestDataGenerator.createUser()
            val performance = PerformanceTestDataGenerator.createPerformance()
            val performanceDateTime = performance.performanceDateTime[0]
            val seatUid = performance.performancePlace.seats[0].uid


            every { userReader.findByUid(user.uid) } returns user
            every { performanceRepository.findPerformanceByUidJoinWithPlaceAndSeat(performance.uid) } returns performance
            every { performanceDateRepository.findByUid(performanceDateTime.uid) } returns performanceDateTime
            every { cartRepository.save(any()) } returns Unit
            every {
                reservationRepository.findReservationBySeatUidAndDateUid(
                    seatUid,
                    performanceDateTime.uid
                )
            } returns null


            reservationService.reservate(user.uid, performance.uid, performanceDateTime.uid, seatUid)

            verify { cartRepository.save(Cart(seatUid, performanceDateTime.uid, user)) }

        }

        "예약시 사용자가 DB에 저장되어 있지 않다면 UnauthorizedException을 throw한다." {
            val userUid = "user001"
            val performanceUid = "perform001"
            val dateUid = "date001"
            val seatUid = "seat001"

            every { userReader.findByUid(userUid) } throws UnAuthorizedException(AuthErrorInfos.USER_INFO_NOT_FOUND)

            val e = shouldThrow<UnAuthorizedException> {
                reservationService.reservate(userUid, performanceUid, dateUid, seatUid)
            }

            e.info shouldBe AuthErrorInfos.USER_INFO_NOT_FOUND
        }

        "예약시 Performance가 DB에 저장되지 않다면 NotFoundException을 throw한다." {
            val user = UserTestDataGenerator.createUser()
            val performanceUid = "perform001"
            val dateUid = "date001"
            val seatUid = "seat001"

            every { userReader.findByUid(user.uid) } returns user
            every { performanceRepository.findPerformanceByUidJoinWithPlaceAndSeat(performanceUid) } returns null

            val e = shouldThrow<NotFoundException> {
                reservationService.reservate(user.uid, performanceUid, dateUid, seatUid)
            }

            e.info shouldBe PerformanceErrorInfos.PERFORMANCE_NOT_FOUND
        }

        "예약시 PerformanceDateTime이 DB에 저장되어 있지 않다면 NotFoundException을 throw한다. " {
            val user = UserTestDataGenerator.createUser()
            val performance = PerformanceTestDataGenerator.createPerformance()
            val dateUid = "date001"
            val seatUid = "seat001"

            every { userReader.findByUid(user.uid) } returns user
            every { performanceRepository.findPerformanceByUidJoinWithPlaceAndSeat(performance.uid) } returns performance
            every { performanceDateRepository.findByUid(dateUid) } returns null

            val e = shouldThrow<NotFoundException> {
                reservationService.reservate(user.uid, performance.uid, dateUid, seatUid)
            }

            e.info shouldBe PerformanceErrorInfos.INVALID_PERFORMANCE_DATE
        }

        "예약시 공연 장소에 seatUid가 포함되어 있지 않다면 InvalidValueException을 throw한다." {
            val user = UserTestDataGenerator.createUser()
            val performance = PerformanceTestDataGenerator.createPerformance()
            val performanceDateTime = performance.performanceDateTime[0]
            val seatUid = "invalidUid"


            every { userReader.findByUid(user.uid) } returns user
            every { performanceRepository.findPerformanceByUidJoinWithPlaceAndSeat(performance.uid) } returns performance
            every { performanceDateRepository.findByUid(performanceDateTime.uid) } returns performanceDateTime

            val e = shouldThrow<InvalidValueException> {
                reservationService.reservate(user.uid, performance.uid, performanceDateTime.uid, seatUid)
            }

            e.info shouldBe PerformanceErrorInfos.PERFORMANCE_SEAT_INFO_INVALID

        }

        "예약 정보가 이미 저장되어 있다면 DuplicatedException을 throw한다." {
            val user = UserTestDataGenerator.createUser()
            val performance = PerformanceTestDataGenerator.createPerformance()
            val performanceDateTime = performance.performanceDateTime[0]
            val seatUid = performance.performancePlace.seats[0].uid
            val reservation = Reservation(
                performanceDateTime,
                performance.performancePlace.seats[0],
                OrderTestDataGenerator.createOrder(user = user)
            )

            every { userReader.findByUid(user.uid) } returns user
            every { performanceRepository.findPerformanceByUidJoinWithPlaceAndSeat(performance.uid) } returns performance
            every { performanceDateRepository.findByUid(performanceDateTime.uid) } returns performanceDateTime
            every { cartRepository.save(any()) } returns Unit
            every {
                reservationRepository.findReservationBySeatUidAndDateUid(
                    seatUid,
                    performanceDateTime.uid
                )
            } returns reservation

            val e = shouldThrow<DuplicatedException> {
                reservationService.reservate(user.uid, performance.uid, performanceDateTime.uid, seatUid)
            }

            e.info shouldBe OrderErrorInfos.ALREADY_RESERVATED
        }

        "예약시 이미 저장된 Cart가 존재한다면  DuplicatedException을 throw한다." {
            val user = UserTestDataGenerator.createUser()
            val performance = PerformanceTestDataGenerator.createPerformance()
            val performanceDateTime = performance.performanceDateTime[0]
            val seatUid = performance.performancePlace.seats[0].uid


            every { userReader.findByUid(user.uid) } returns user
            every { performanceRepository.findPerformanceByUidJoinWithPlaceAndSeat(performance.uid) } returns performance
            every { performanceDateRepository.findByUid(performanceDateTime.uid) } returns performanceDateTime
            every { cartRepository.save(any()) } returns Unit
            every {
                reservationRepository.findReservationBySeatUidAndDateUid(
                    seatUid,
                    performanceDateTime.uid
                )
            } returns null
            every { cartRepository.save(any()) } throws DataIntegrityViolationException("중복!")


            val e = shouldThrow<DuplicatedException> {
                reservationService.reservate(user.uid, performance.uid, performanceDateTime.uid, seatUid)
            }

            e.info shouldBe OrderErrorInfos.ALREADY_RESERVATED


        }
    }

}