package com.example.receiptscanner.ui.components

import android.content.Context
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

private val Context.transactionsDataStore by dataStore(
    fileName = "transactions.json",
    serializer = TransactionsSerializer
)

class TransactionsRepository(private val context: Context) {
    val transactions: Flow<List<Transaction>> = context.transactionsDataStore.data
        .map { serializableTransactions ->
            serializableTransactions.map { it.toTransaction() }
        }

    suspend fun addTransaction(transaction: Transaction) {
        context.transactionsDataStore.updateData { currentTransactions ->
            currentTransactions + transaction.toSerializable()
        }
    }


    suspend fun deleteTransaction(transactionId: String) {
        context.transactionsDataStore.updateData { currentTransactions ->
            currentTransactions.filterNot { it.id == transactionId }
        }
    }

    suspend fun updateTransaction(transactionId: String, updatedTransaction: Transaction) {
        context.transactionsDataStore.updateData { currentTransactions ->
            currentTransactions.map { transaction ->
                if (transaction.id == transactionId) {
                    // Preserve the original ID when updating
                    when (updatedTransaction) {
                        is TransactionSpend -> updatedTransaction.copy(id = transactionId)
                        is TransactionIncome -> updatedTransaction.copy(id = transactionId)
                    }.toSerializable()
                } else {
                    transaction
                }
            }
        }
    }


    suspend fun initializeWithSampleData() {
        context.transactionsDataStore.updateData { currentTransactions ->
            currentTransactions.ifEmpty {
                getSampleTransactions().map { it.toSerializable() }
            }
        }
    }

    private fun SerializableTransaction.toTransaction(): Transaction =
        when (type) {
            "SPEND" -> TransactionSpend(
                id = id,
                description = description,
                totalAmount = totalAmount,
                addressLocation = addressLocation ?: "",
                phoneNumberOfMerchant = phoneNumberOfMerchant ?: "",
                timestamp = LocalDateTime.parse(timestamp),
                items = items
            )
            "INCOME" -> TransactionIncome(
                id = id,
                description = description,
                totalAmount = totalAmount,
                timestamp = LocalDateTime.parse(timestamp)
            )
            else -> throw IllegalStateException("Unknown transaction type: $type")
        }

    private fun Transaction.toSerializable(): SerializableTransaction =
        when (this) {
            is TransactionSpend -> SerializableTransaction(
                id = id,
                type = "SPEND",
                description = description,
                totalAmount = totalAmount,
                addressLocation = addressLocation,
                phoneNumberOfMerchant = phoneNumberOfMerchant,
                timestamp = timestamp.toString(),
                items = items
            )
            is TransactionIncome -> SerializableTransaction(
                id = id,
                type = "INCOME",
                description = description,
                totalAmount = totalAmount,
                timestamp = timestamp.toString()
            )
        }

    suspend fun clearAllTransactions() {
        context.transactionsDataStore.updateData {
            emptyList()
        }
    }

    suspend fun clearAndResetData() {
        context.transactionsDataStore.updateData {
            // Clear existing data and add sample data
            getSampleTransactions().map { it.toSerializable() }
        }
    }
}