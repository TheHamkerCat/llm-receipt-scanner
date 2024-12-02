package com.example.receiptscanner.ui.components

import java.time.LocalDateTime

fun validateInput(
    isSpending: Boolean,
    description: String,
    amount: String,
    addressLocation: String,
    phoneNumber: String
): Boolean {
    return if (isSpending) {
        description.isNotBlank() &&
                amount.isNotBlank() &&
                addressLocation.isNotBlank() &&
                phoneNumber.isNotBlank()
    } else {
        description.isNotBlank() && amount.isNotBlank()
    }
}


fun calculateMonthTotal(transactions: List<Transaction>): Double =
    transactions
        .filter { it.timestamp.monthValue == LocalDateTime.now().monthValue }
        .sumOf {
            when (it) {
                is TransactionSpend -> -it.totalAmount
                is TransactionIncome -> it.totalAmount
            }
        }

fun calculateWeekTotal(transactions: List<Transaction>): Double =
    transactions
        .filter { it.timestamp.isAfter(LocalDateTime.now().minusWeeks(1)) }
        .sumOf {
            when (it) {
                is TransactionSpend -> -it.totalAmount
                is TransactionIncome -> it.totalAmount
            }
        }

