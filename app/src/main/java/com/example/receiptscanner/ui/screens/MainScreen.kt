package com.example.receiptscanner.ui.screens

import android.content.Context
import kotlinx.serialization.json.*
import java.time.format.DateTimeFormatter
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.receiptscanner.ui.components.AnthropicApiService
import com.example.receiptscanner.ui.components.CurrencySettingsDialog
import com.example.receiptscanner.ui.components.ImagePickerBottomSheet
import com.example.receiptscanner.ui.components.ManualEntryDialog
import java.time.LocalDateTime
import java.util.*



import com.example.receiptscanner.ui.components.Transaction
import com.example.receiptscanner.ui.components.TransactionSpend
import com.example.receiptscanner.ui.components.TransactionsContent
import com.example.receiptscanner.ui.components.TransactionsRepository
import com.example.receiptscanner.ui.components.calculateMonthTotal
import com.example.receiptscanner.ui.components.calculateWeekTotal
import com.example.receiptscanner.ui.components.formatAmount
import com.example.receiptscanner.ui.components.toSearchableText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


// Color definitions
val DarkGreen = Color(0xFF006400)
val ExpenseRed = Color(0xFFB22222)

object ApiKeyPreferences {
    private val Context.apiKeyDataStore by preferencesDataStore(name = "api_key_settings")
    private val API_KEY = stringPreferencesKey("api_key")

    suspend fun saveApiKey(context: Context, apiKey: String) {
        context.apiKeyDataStore.edit { preferences ->
            preferences[API_KEY] = apiKey
        }
    }

    suspend fun getApiKey(context: Context): String {
        return context.apiKeyDataStore.data.first()[API_KEY] ?: ""
    }
}

val supportedCurrencies = listOf(
    "USD" to Locale.US,
    "EUR" to Locale("en", "DE"),
    "INR" to Locale("en", "IN")
)

@Composable
fun MainReceiptScanner(transactionsRepository: TransactionsRepository) {
    var apiKey by remember { mutableStateOf("") }

    var searchQuery by remember { mutableStateOf("") }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var selectedLocale by remember { mutableStateOf(Locale("en", "IN")) }
    var isManualEntryOpen by remember { mutableStateOf(false) }

    var selectedTransactionForEdit by remember { mutableStateOf<Transaction?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf<Transaction?>(null) }


    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Collect transactions from DataStore
    val transactions by transactionsRepository.transactions.collectAsState(initial = emptyList())

    // Initialize sample data on first launch (then save in datastore)
    LaunchedEffect(Unit) {
        transactionsRepository.initializeWithSampleData()
    }

    LaunchedEffect(Unit) {
        apiKey = ApiKeyPreferences.getApiKey(context)
    }


    // Filtered transactions
    val filteredTransactions = remember(searchQuery, transactions) {
        if (searchQuery.isBlank()) {
            transactions.sortedByDescending { it.timestamp }
        } else {
            val searchTerms = searchQuery.lowercase().split(" ")
            transactions.filter { transaction ->
                val searchableText = transaction.toSearchableText()
                // Match if ALL search terms are found in the searchable text
                searchTerms.all { term ->
                    searchableText.contains(term)
                }
            }.sortedByDescending { it.timestamp }
        }
    }

    val thisMonthTotal = calculateMonthTotal(transactions)
    val thisWeekTotal = calculateWeekTotal(transactions)

    ReceiptScannerScreen(
        searchQuery = searchQuery,
        onSearchQueryChange = { searchQuery = it },
        isSettingsOpen = isSettingsOpen,
        onSettingsOpenChange = { isSettingsOpen = it },
        selectedLocale = selectedLocale,
        onLocaleSelected = { selectedLocale = it },
        isManualEntryOpen = isManualEntryOpen,
        onManualEntryOpenChange = { isManualEntryOpen = it },
        filteredTransactions = filteredTransactions,
        thisMonthTotal = thisMonthTotal,
        thisWeekTotal = thisWeekTotal,
        selectedTransactionForEdit = selectedTransactionForEdit,
        onEditTransaction = { selectedTransactionForEdit = it },
        onUpdateTransaction = { transaction ->
            scope.launch {
                transactionsRepository.updateTransaction(transaction.id, transaction)
                selectedTransactionForEdit = null
            }
        },
        showDeleteConfirmation = showDeleteConfirmation,
        onDeleteConfirmationChange = { showDeleteConfirmation = it },
        onDeleteTransaction = { transaction ->
            scope.launch {
                transactionsRepository.deleteTransaction(transaction.id)
                showDeleteConfirmation = null
            }
        },
        onTransactionAdded = { newTransaction ->
            scope.launch {
                transactionsRepository.addTransaction(newTransaction)
            }
        },
        onClearData = {
            scope.launch {
                transactionsRepository.clearAndResetData()
            }
        },
        apiKey = apiKey,
        onApiKeyChange = { newKey ->
            apiKey = newKey
            scope.launch {
                ApiKeyPreferences.saveApiKey(context, newKey)
            }
        }
    )

    // Show delete confirmation dialog
    DeleteConfirmationDialog(
        transaction = showDeleteConfirmation,
        onDismiss = { showDeleteConfirmation = null },
        onConfirmDelete = { transaction ->
            scope.launch {
                transactionsRepository.deleteTransaction(transaction.id)
                showDeleteConfirmation = null
            }
        }
    )


    // Show edit dialog
    if (selectedTransactionForEdit != null) {
        ManualEntryDialog(
            isOpen = true,
            onDismiss = { selectedTransactionForEdit = null },
            selectedLocale = selectedLocale,
            editTransaction = selectedTransactionForEdit,
            onTransactionUpdated = { transaction ->
                scope.launch {
                    transactionsRepository.updateTransaction(transaction.id, transaction)
                    selectedTransactionForEdit = null
                }
            }
        )
    }

}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScannerScreen(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isSettingsOpen: Boolean,
    onSettingsOpenChange: (Boolean) -> Unit,
    selectedLocale: Locale,
    onLocaleSelected: (Locale) -> Unit,
    isManualEntryOpen: Boolean,
    onManualEntryOpenChange: (Boolean) -> Unit,
    filteredTransactions: List<Transaction>,
    thisMonthTotal: Double,
    thisWeekTotal: Double,
    selectedTransactionForEdit: Transaction?,
    onEditTransaction: (Transaction) -> Unit,
    onUpdateTransaction: (Transaction) -> Unit,
    showDeleteConfirmation: Transaction?,
    onDeleteConfirmationChange: (Transaction?) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    onTransactionAdded: (Transaction) -> Unit,
    onClearData: () -> Unit,
    apiKey: String,
    onApiKeyChange: (String) -> Unit
) {
    Scaffold(
        topBar = {
            ReceiptScannerTopBar(
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onSettingsClick = { onSettingsOpenChange(true) },
                thisMonthTotal = thisMonthTotal,
                thisWeekTotal = thisWeekTotal,
                selectedLocale = selectedLocale,
                onAddTransactionClick = { onManualEntryOpenChange(true) },
                onAutoTransactionAdded = onTransactionAdded,
            )
        },

    ) { paddingValues ->
        TransactionsContent(
            paddingValues = paddingValues,
            filteredTransactions = filteredTransactions,
            selectedLocale = selectedLocale,
            onEditTransaction = onEditTransaction,
            onDeleteTransaction = { onDeleteConfirmationChange(it) }
        )
    }

    // Dialogs
    CurrencySettingsDialog(
        isOpen = isSettingsOpen,
        onDismiss = { onSettingsOpenChange(false) },
        selectedLocale = selectedLocale,
        onLocaleSelected = onLocaleSelected,
        onClearData = onClearData,
        apiKey = apiKey,
        onApiKeyChange = onApiKeyChange
    )

    // Manual Entry Dialog for new transactions
    if (isManualEntryOpen) {
        ManualEntryDialog(
            isOpen = true,
            onDismiss = { onManualEntryOpenChange(false) },
            selectedLocale = selectedLocale,
            onTransactionAdded = onTransactionAdded
        )
    }

    // Manual Entry Dialog for editing
    if (selectedTransactionForEdit != null) {
        ManualEntryDialog(
            isOpen = true,
            onDismiss = { onEditTransaction(selectedTransactionForEdit) },
            selectedLocale = selectedLocale,
            editTransaction = selectedTransactionForEdit,
            onTransactionUpdated = onUpdateTransaction
        )
    }

    // Delete confirmation dialog
    DeleteConfirmationDialog(
        transaction = showDeleteConfirmation,
        onDismiss = { onDeleteConfirmationChange(null) },
        onConfirmDelete = onDeleteTransaction
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScannerTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSettingsClick: () -> Unit,
    thisMonthTotal: Double,
    thisWeekTotal: Double,
    selectedLocale: Locale,
    onAddTransactionClick: () -> Unit,
    onAutoTransactionAdded: (Transaction) -> Unit,
) {
    var uploadProgress by remember { mutableStateOf(0f) }
    var isLoading by remember { mutableStateOf(false) }

    var showAddMenu by remember { mutableStateOf(false) }
    var showImagePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val anthropicService = remember { AnthropicApiService(context) }

    ImagePickerBottomSheet(
        show = showImagePicker,
        onDismiss = {
            showImagePicker = false
            uploadProgress = 0f
            isLoading = false
        },
        onImageSelected = { uri ->
            isLoading = true
            val result = anthropicService.analyzeReceiptImage(uri) { progress ->
                uploadProgress = progress
            }
            println(result)

            // Toast.makeText(context, result, Toast.LENGTH_LONG).show()

            try {
                val jsonObject = Json.decodeFromString<JsonObject>(result)

                val itemsMap = try {
                    val itemsArray = jsonObject["items"]?.jsonArray
                    println("Items array: $itemsArray") // Debug print

                    if (!itemsArray.isNullOrEmpty()) {
                        itemsArray.mapNotNull { item ->
                            try {
                                val itemObject = item.jsonObject
                                println("Item object: $itemObject") // Debug print

                                if (itemObject.entries.isEmpty()) {
                                    println("Empty item object found")
                                    null
                                } else {
                                    val entry = itemObject.entries.first()
                                    entry.key to (entry.value.jsonPrimitive.doubleOrNull ?: 0.0)
                                }
                            } catch (e: Exception) {
                                println("Error parsing individual item: $item")
                                println("Error: ${e.message}")
                                null
                            }
                        }.toMap()
                    } else {
                        emptyMap()
                    }
                } catch (e: Exception) {
                    println("Error parsing items: ${e.message}")
                    emptyMap()
                }

                val description = jsonObject["description"]?.jsonPrimitive?.content
                val totalAmount = jsonObject["totalAmount"]?.jsonPrimitive?.double
                val addressLocation = jsonObject["addressLocation"]?.jsonPrimitive?.content
                val phoneNumber = jsonObject["phoneNumberOfMerchant"]?.jsonPrimitive?.content

                // Only try to parse timestamp if it exists
                val timestamp = jsonObject["timestamp"]?.jsonPrimitive?.content?.let { timestampStr ->
                    try {
                        LocalDateTime.parse(timestampStr, DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"))
                    } catch (e: Exception) {
                        println("Couldn't parse timestamp: $timestampStr")
                        LocalDateTime.now()
                    }
                } ?: LocalDateTime.now() // Use current time if timestamp field doesn't exist


                // Check if all essential fields are empty
                if (!(description.isNullOrBlank() && totalAmount == null)) {
                    val transaction = TransactionSpend(
                        description = description ?: "Untitled Receipt",
                        totalAmount = totalAmount ?: 0.0,
                        addressLocation = addressLocation ?: "",
                        phoneNumberOfMerchant = phoneNumber ?: "",
                        timestamp = timestamp ?: LocalDateTime.now(),
                        items = itemsMap
                    )
                    onAutoTransactionAdded(transaction)
                    Toast.makeText(context, "Receipt added successfully", Toast.LENGTH_SHORT).show()
                }
                else {
                    Toast.makeText(
                        context,
                        "Couldn't get receipt from provided image",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                Toast.makeText(context, "Error parsing receipt: ${e.message}", Toast.LENGTH_LONG).show()
                uploadProgress = 0f
            } finally {
                isLoading = false
            }
        }
    )

    Column {
        TopAppBar(
            title = { Text("Receipt Scanner", fontSize = 16.sp) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Black,
                titleContentColor = Color.White,
                actionIconContentColor = Color.White
            ),
            actions = {
                Box {
                    IconButton(onClick = { showAddMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Transaction",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showAddMenu,
                        onDismissRequest = { showAddMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Manual Entry") },
                            onClick = {
                                onAddTransactionClick()
                                showAddMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, "Manual Entry")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Scan Receipt") },
                            onClick = {
                                showImagePicker = true
                                showAddMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.CameraAlt, "Scan Receipt")
                            }
                        )
                    }
                }

                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = "Settings",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        )
        SpendingSummaryCards(thisMonthTotal, thisWeekTotal, selectedLocale)
        SearchBar(
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange
        )


        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
        ) {
            if (isLoading) {  // Single condition for the Box content
                if (uploadProgress > 0 && uploadProgress < 1) {
                    // Show determinate progress when we have a progress value
                    LinearProgressIndicator(
                        progress = uploadProgress,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    // Show indeterminate progress when loading but no progress value
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Spinner overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 20.dp, top = 20.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                }
            }
        }



    }
}

@Composable
fun SpendingSummaryCards(monthTotal: Double, weekTotal: Double, locale: Locale) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFf0f0f0),
        ),
        shape = RoundedCornerShape(
            topStart = 0.dp,
            topEnd = 0.dp,
            bottomStart = 0.dp,
            bottomEnd = 0.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp,
                top = 16.dp,
            ),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // This Month's Summary
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "This Month",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF4a4a4a),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${if (monthTotal >= 0) "+ " else ""}${formatAmount(monthTotal, locale)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (monthTotal >= 0) DarkGreen else ExpenseRed,
                    fontSize = 18.sp,
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            // This Week's Summary
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "This Week",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF4a4a4a),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${if (weekTotal >= 0) "+ " else ""}${formatAmount(weekTotal, locale)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (weekTotal >= 0) DarkGreen else ExpenseRed,
                    fontSize = 18.sp,
                )
            }
        }
        // Bottom border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Color(0xFFe0e0e0))
        )
    }
}

@Composable
fun SearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    TextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("Search transactions") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                }
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFf0f0f0),
            unfocusedContainerColor = Color(0xFFf0f0f0),
            focusedIndicatorColor = Color.Red,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        shape = RoundedCornerShape(8.dp),
        singleLine = true
    )
}


@Composable
fun DeleteConfirmationDialog(
    transaction: Transaction?,
    onDismiss: () -> Unit,
    onConfirmDelete: (Transaction) -> Unit
) {
    if (transaction != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Delete Transaction") },
            text = {
                Text("Are you sure you want to delete this transaction: ${transaction.description}? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = { onConfirmDelete(transaction) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}



