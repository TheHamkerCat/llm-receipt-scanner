package com.example.receiptscanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.receiptscanner.ui.screens.DarkGreen
import com.example.receiptscanner.ui.screens.ExpenseRed
import com.example.receiptscanner.ui.screens.supportedCurrencies
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID


@Composable
fun CurrencySettingsDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    selectedLocale: Locale,
    onLocaleSelected: (Locale) -> Unit,
    onClearData: () -> Unit,
    apiKey: String,
    onApiKeyChange: (String) -> Unit
) {
    if (isOpen) {
        var showConfirmDialog by remember { mutableStateOf(false) }
        var isApiKeyVisible by remember { mutableStateOf(false) }

        // Main Settings Dialog
        AlertDialog(
            shape = RoundedCornerShape(5.dp),
            onDismissRequest = onDismiss,
            title = { Text("Settings") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (apiKey.isNotEmpty()) {
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = onApiKeyChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("API Key") },
                            visualTransformation = if (isApiKeyVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation('•')
                            },
                            trailingIcon = {
                                IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                    Icon(
                                        imageVector = if (isApiKeyVisible)
                                            Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (isApiKeyVisible)
                                            "Hide API Key" else "Show API Key"
                                    )
                                }
                            },
//                            singleLine = true
                        )
                    } else {
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = onApiKeyChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("API Key") },
                            placeholder = { Text("Enter your Claude Or OpenAI API key") },
//                            singleLine = true
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))



                    // Currency section
                    Text(
                        "Currency Settings",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    supportedCurrencies.forEach { (currency, locale) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${locale.displayCountry} ($currency)",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Switch(
                                checked = selectedLocale == locale,
                                onCheckedChange = { if (it) onLocaleSelected(locale) }
                            )
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // Data Management section
                    Text(
                        "Data Management",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Button(
                        onClick = { showConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Reset",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset to Sample Data")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        )

        // Confirmation Dialog
        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = { Text("Confirm Reset Data") },
                text = { Text("Are you sure you want to reset all transactions to sample data? This will remove all your current transactions. This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onClearData()
                            showConfirmDialog = false
                            onDismiss()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Reset Data")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun ManualEntryDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    selectedLocale: Locale,
    editTransaction: Transaction? = null,
    onTransactionAdded: ((Transaction) -> Unit)? = null,
    onTransactionUpdated: ((Transaction) -> Unit)? = null
) {
    if (!isOpen) return

    var isSpending by remember(editTransaction) {
        mutableStateOf(editTransaction is TransactionSpend)
    }
    var description by remember(editTransaction) {
        mutableStateOf(editTransaction?.description ?: "")
    }
    var amount by remember(editTransaction) {
        mutableStateOf(editTransaction?.totalAmount?.toString() ?: "")
    }
    var addressLocation by remember(editTransaction) {
        mutableStateOf((editTransaction as? TransactionSpend)?.addressLocation ?: "")
    }
    var phoneNumber by remember(editTransaction) {
        mutableStateOf((editTransaction as? TransactionSpend)?.phoneNumberOfMerchant ?: "")
    }
    var items by remember(editTransaction) {
        mutableStateOf((editTransaction as? TransactionSpend)?.items?.toMutableMap() ?: mutableMapOf())
    }
    var newItemName by remember { mutableStateOf("") }
    var newItemPrice by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editTransaction != null) "Edit Transaction" else "Add Transaction") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Transaction Type Selection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            width = 1.dp,
                            color = Color(0xFFE0E0E0),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TransactionTypeButton(
                        modifier = Modifier.weight(1f),
                        selected = isSpending,
                        onClick = { isSpending = true },
                        icon = Icons.Default.CreditCard,
                        text = "Expense"
                    )
                    TransactionTypeButton(
                        modifier = Modifier.weight(1f),
                        selected = !isSpending,
                        onClick = { isSpending = false },
                        icon = Icons.Default.AccountBalanceWallet,
                        text = "Income"
                    )
                }

                // Basic Fields
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = hasError && description.isBlank()
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) amount = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    isError = hasError && amount.isBlank()
                )

                // Additional fields for spending
                if (isSpending) {
                    OutlinedTextField(
                        value = addressLocation,
                        onValueChange = { addressLocation = it },
                        label = { Text("Location") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = hasError && addressLocation.isBlank()
                    )

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Merchant Phone") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = hasError && phoneNumber.isBlank()
                    )

                    // Items Section
                    Text(
                        "Line Items",
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(top = 10.dp)
                    )

                    // Add new item
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newItemName,
                            onValueChange = { newItemName = it },
                            label = { Text("Item") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newItemPrice,
                            onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) newItemPrice = it },
                            label = { Text("Price") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            modifier = Modifier.padding(top = 10.dp),
                            onClick = {
                                if (newItemName.isNotBlank() && newItemPrice.isNotBlank()) {
                                    items[newItemName] = newItemPrice.toDouble()
                                    newItemName = ""
                                    newItemPrice = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, "Add Item")
                        }
                    }

                    // Display items
                    items.forEach { (name, price) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("$name: ${formatAmount(price, selectedLocale)}")
                            IconButton(
                                onClick = { items.remove(name) }
                            ) {
                                Icon(Icons.Default.Close, "Remove Item")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (validateInput(isSpending, description, amount, addressLocation, phoneNumber)) {
                        val newTransaction = if (isSpending) {
                            TransactionSpend(
                                id = editTransaction?.id ?: UUID.randomUUID().toString(),
                                description = description,
                                totalAmount = amount.toDouble(),
                                addressLocation = addressLocation,
                                phoneNumberOfMerchant = phoneNumber,
                                timestamp = editTransaction?.timestamp ?: LocalDateTime.now(),
                                items = items.toMap()
                            )
                        } else {
                            TransactionIncome(
                                id = editTransaction?.id ?: UUID.randomUUID().toString(),
                                description = description,
                                totalAmount = amount.toDouble(),
                                timestamp = editTransaction?.timestamp ?: LocalDateTime.now()
                            )
                        }

                        if (editTransaction != null) {
                            onTransactionUpdated?.invoke(newTransaction)
                        } else {
                            onTransactionAdded?.invoke(newTransaction)
                        }
                        onDismiss()
                    } else {
                        hasError = true
                    }
                }
            ) {
                Text(if (editTransaction != null) "Update" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


@Composable
private fun TransactionTypeButton(
    modifier: Modifier = Modifier,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    text: String
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(
                if (selected) {
                    if (text == "Expense") ExpenseRed.copy(alpha = 0.1f)
                    else DarkGreen.copy(alpha = 0.1f)
                } else Color.Transparent
            )
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) {
                    if (text == "Expense") ExpenseRed else DarkGreen
                } else Color.Transparent,
                shape = RoundedCornerShape(
                    topStart = if (text == "Expense") 8.dp else 0.dp,
                    bottomStart = if (text == "Expense") 8.dp else 0.dp,
                    topEnd = if (text == "Income") 8.dp else 0.dp,
                    bottomEnd = if (text == "Income") 8.dp else 0.dp
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) {
                    if (text == "Expense") ExpenseRed else DarkGreen
                } else Color.Gray,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text,
                color = if (selected) {
                    if (text == "Expense") ExpenseRed else DarkGreen
                } else Color.Gray,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
