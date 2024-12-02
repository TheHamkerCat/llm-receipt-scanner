package com.example.receiptscanner.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.receiptscanner.ui.screens.DarkGreen
import com.example.receiptscanner.ui.screens.ExpenseRed
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID



@Composable
fun TransactionsContent(
    paddingValues: PaddingValues,
    filteredTransactions: List<Transaction>,
    selectedLocale: Locale,
    onEditTransaction: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(paddingValues)
            .padding(horizontal = 16.dp)
            .fillMaxSize()
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        TransactionsList(
            transactions = filteredTransactions,
            locale = selectedLocale,
            onEditTransaction = onEditTransaction,
            onDeleteTransaction = onDeleteTransaction
        )
    }
}

@Composable
fun TransactionsList(
    transactions: List<Transaction>,
    locale: Locale,
    onEditTransaction: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit
) {
    val groupedTransactions = transactions.groupBy {
        it.timestamp.toLocalDate()
    }

    LazyColumn {
        groupedTransactions.forEach { (date, dailyTransactions) ->
            item {
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(dailyTransactions) { transaction ->
                TransactionItem(
                    transaction = transaction,
                    locale = locale,
                    onEditClick = onEditTransaction,
                    onDeleteClick = onDeleteTransaction
                )
                Divider(
                    color = Color(0xFFE0E0E0),
                    thickness = 1.dp
                )
            }
        }
    }
}


@Composable
fun TransactionItem(
    transaction: Transaction,
    locale: Locale,
    onEditClick: (Transaction) -> Unit,
    onDeleteClick: (Transaction) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Existing transaction content
            Row(
                modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (transaction) {
                        is TransactionSpend -> Icons.Default.CreditCard
                        is TransactionIncome -> Icons.Default.AccountBalanceWallet
                    },
                    contentDescription = null,
                    tint = when (transaction) {
                        is TransactionSpend -> ExpenseRed
                        is TransactionIncome -> DarkGreen
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = transaction.timestamp.format(DateTimeFormatter.ofPattern("HH:mm")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Amount and menu
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatAmount(
                        when (transaction) {
                            is TransactionSpend -> -transaction.totalAmount
                            is TransactionIncome -> transaction.totalAmount
                        },
                        locale
                    ),
                    color = when (transaction) {
                        is TransactionSpend -> ExpenseRed
                        is TransactionIncome -> DarkGreen
                    },
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                onEditClick(transaction)
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, "Edit")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                onDeleteClick(transaction)
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, "Delete")
                            }
                        )
                    }
                }
            }
        }


        // Expandable content (only for TransactionSpend)
        if (transaction is TransactionSpend && isExpanded) {
            ExpandedTransactionDetails(transaction, locale)
        }
    }
}



@Composable
private fun ExpandedTransactionDetails(
    transaction: TransactionSpend,
    locale: Locale
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 32.dp, top = 8.dp, bottom = 12.dp, end = 24.dp)
    ) {
        // Location info
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Location",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = transaction.addressLocation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Phone info
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = "Phone",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = transaction.phoneNumberOfMerchant,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Items section
        if (transaction.items.isNotEmpty()) {
            Text(
                text = "Items",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Items table
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .clip(RoundedCornerShape(4.dp))
            ) {
                transaction.items.forEach { (item, price) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatAmount(price, locale),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (item != transaction.items.keys.last()) {
                        Divider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}


fun getSampleTransactions(): List<Transaction> = listOf(
    // Today's transactions
    TransactionSpend(
        description = "Grocery Shopping",
        totalAmount = 850.50,
        addressLocation = "123 Market Street",
        phoneNumberOfMerchant = "+1234567890",
        timestamp = LocalDateTime.now().withHour(13).withMinute(30), // Today at 1:30 PM
        items = mapOf(
            "Milk" to 45.00,
            "Bread" to 35.50,
            "Vegetables" to 770.00
        )
    ),
    TransactionIncome(
        description = "Monthly Salary",
        totalAmount = 25000.00,
        timestamp = LocalDateTime.now().withHour(10).withMinute(0) // Today at 10:00 AM
    ),
    TransactionSpend(
        description = "Electronics Store",
        totalAmount = 1299.99,
        addressLocation = "789 Tech Boulevard",
        phoneNumberOfMerchant = "+1122334455",
        timestamp = LocalDateTime.now().withHour(16).withMinute(45), // Today at 4:45 PM
        items = mapOf(
            "Wireless Earbuds" to 799.99,
            "Phone Case" to 299.00,
            "Screen Protector" to 201.00
        )
    ),

    // Yesterday's transactions
    TransactionSpend(
        description = "Restaurant Dinner",
        totalAmount = 457.75,
        addressLocation = "456 Food Court",
        phoneNumberOfMerchant = "+9876543210",
        timestamp = LocalDateTime.now().minusDays(1).withHour(19).withMinute(45), // Yesterday at 7:45 PM
        items = mapOf(
            "Main Course" to 350.00,
            "Dessert" to 107.75
        )
    ),
    TransactionIncome(
        description = "Freelance Project",
        totalAmount = 5000.00,
        timestamp = LocalDateTime.now().minusDays(1).withHour(14).withMinute(15) // Yesterday at 2:15 PM
    ),
    TransactionSpend(
        description = "Bookstore",
        totalAmount = 625.50,
        addressLocation = "321 Reading Lane",
        phoneNumberOfMerchant = "+5544332211",
        timestamp = LocalDateTime.now().minusDays(1).withHour(11).withMinute(30), // Yesterday at 11:30 AM
        items = mapOf(
            "Programming Book" to 399.50,
            "Novel" to 126.00,
            "Magazine" to 100.00
        )
    ),
    TransactionIncome(
        description = "Investment Dividend",
        totalAmount = 1250.00,
        timestamp = LocalDateTime.now().minusDays(1).withHour(9).withMinute(0) // Yesterday at 9:00 AM
    )
)


fun Transaction.toSearchableText(): String {
    return buildString {
        append(description)
        append(" ")
        append(formatAmount(when (this@toSearchableText) {
            is TransactionSpend -> totalAmount
            is TransactionIncome -> totalAmount
        }, Locale.getDefault()))
        append(" ")
        append(timestamp.format(DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm")))

        if (this@toSearchableText is TransactionSpend) {
            append(" ")
            append(addressLocation)
            append(" ")
            append(phoneNumberOfMerchant)
            // Add items to searchable text
            items.forEach { (itemName, itemPrice) ->
                append(" ")
                append(itemName)
                append(" ")
                append(formatAmount(itemPrice, Locale.getDefault()))
            }
        }
    }.lowercase()
}

fun formatAmount(amount: Double, locale: Locale): String {
    val format = NumberFormat.getCurrencyInstance(locale)
    return format.format(amount)
}
