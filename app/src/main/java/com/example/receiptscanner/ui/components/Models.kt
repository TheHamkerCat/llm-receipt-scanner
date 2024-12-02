package com.example.receiptscanner.ui.components

import java.time.LocalDateTime
import java.util.UUID

sealed class Transaction {
    abstract val id: String
    abstract val description: String
    abstract val totalAmount: Double
    abstract val timestamp: LocalDateTime
}

data class TransactionSpend(
    override val id: String = UUID.randomUUID().toString(),
    override val description: String,
    override val totalAmount: Double,
    val addressLocation: String,
    val phoneNumberOfMerchant: String,
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    val items: Map<String, Double> // itemName to price mapping
) : Transaction()

data class TransactionIncome(
    override val id: String = UUID.randomUUID().toString(),
    override val description: String,
    override val totalAmount: Double,
    override val timestamp: LocalDateTime = LocalDateTime.now()
) : Transaction()

