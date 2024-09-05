package com.flab.ticketing.order.repository

import com.flab.ticketing.order.entity.Order
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface OrderRepository : CrudRepository<Order, Long> {

    @Query("SELECT o FROM Order o WHERE o.user.uid = :userUid")
    fun findByUser(@Param("userUid") userUid: String): List<Order>

    fun findByUid(uid: String): Order?


    @Query(
        "SELECT o FROM Order o " +
                "WHERE o.user.uid = :userUid " +
                "ORDER BY o.id DESC"
    )
    fun findByUser(@Param("userUid") userUid: String, Pageable: Pageable): List<Order>


    @Query(
        "SELECT o FROM Order o " +
                "WHERE o.user.uid = :userUid AND " +
                "o.id <= (SELECT o.id FROM Order o WHERE o.uid = :cursor) " +
                "ORDER BY o.id DESC"
    )
    fun findByUser(@Param("userUid") userUid: String, @Param("cursor") cursor: String, Pageable: Pageable): List<Order>
}