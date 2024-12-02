package com.example.receiptscanner.ui.components

import androidx.datastore.core.Serializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

@Serializable
data class SerializableTransaction(
    val id: String,
    val type: String, // "SPEND" or "INCOME"
    val description: String,
    val totalAmount: Double,
    val addressLocation: String? = null,
    val phoneNumberOfMerchant: String? = null,
    val timestamp: String, // ISO formatted datetime
    val items: Map<String, Double> = emptyMap()
)

object TransactionsSerializer : Serializer<List<SerializableTransaction>> {
    override val defaultValue: List<SerializableTransaction> = emptyList()

    override suspend fun readFrom(input: InputStream): List<SerializableTransaction> {
        return try {
            Json.decodeFromString(input.readBytes().decodeToString())
        } catch (e: Exception) {
            defaultValue
        }
    }

    override suspend fun writeTo(t: List<SerializableTransaction>, output: OutputStream) {
        withContext(Dispatchers.IO) {
            output.write(Json.encodeToString(t).encodeToByteArray())
        }
    }
}
