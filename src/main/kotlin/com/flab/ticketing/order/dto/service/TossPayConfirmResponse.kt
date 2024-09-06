package com.flab.ticketing.order.dto.service

data class TossPayConfirmResponse(
    val mId: String,
    val lastTransactionKey: String,
    val paymentKey: String,
    val orderId: String,
    val orderName: String,
    val taxExemptionAmount: Int,
    val status: String,
    val requestedAt: String,
    val approvedAt: String,
    val useEscrow: Boolean,
    val cultureExpense: Boolean,
    val card: Card?,
    val virtualAccount: Any?,
    val transfer: Any?,
    val mobilePhone: Any?,
    val giftCertificate: Any?,
    val cashReceipt: Any?,
    val cashReceipts: Any?,
    val discount: Any?,
    val cancels: Any?,
    val secret: Any?,
    val type: String,
    val easyPay: EasyPay?,
    val easyPayAmount: Int,
    val easyPayDiscountAmount: Int,
    val country: String,
    val failure: Any?,
    val isPartialCancelable: Boolean,
    val receipt: Receipt?,
    val checkout: Checkout?,
    val currency: String,
    val totalAmount: Int,
    val balanceAmount: Int,
    val suppliedAmount: Int,
    val vat: Int,
    val taxFreeAmount: Int,
    val method: String,
    val version: String
){
    data class Card(
        val issuerCode: String,
        val acquirerCode: String,
        val number: String,
        val installmentPlanMonths: Int,
        val isInterestFree: Boolean,
        val interestPayer: Any?,
        val approveNo: String,
        val useCardPoint: Boolean,
        val cardType: String,
        val ownerType: String,
        val acquireStatus: String,
        val receiptUrl: String,
        val amount: Int
    )

    data class EasyPay(
        val provider: String,
        val amount: Int,
        val discountAmount: Int
    )

    data class Receipt(
        val url: String
    )

    data class Checkout(
        val url: String
    )



}

