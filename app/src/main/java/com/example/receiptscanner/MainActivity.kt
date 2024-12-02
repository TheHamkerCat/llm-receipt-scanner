package com.example.receiptscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.example.receiptscanner.ui.components.TransactionsRepository
import com.example.receiptscanner.ui.screens.MainReceiptScanner
import com.example.receiptscanner.ui.theme.ReceiptScannerTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ReceiptScannerTheme {
                Surface {
                    val transactionsRepository = TransactionsRepository(this)
                    MainReceiptScanner(transactionsRepository)
                }
            }
        }
    }
}
