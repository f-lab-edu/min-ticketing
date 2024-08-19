package com.flab.ticketing.order.repository

import com.flab.ticketing.order.entity.Reservation
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository


@Repository
interface ReservationRepository : CrudRepository<Reservation, Long>