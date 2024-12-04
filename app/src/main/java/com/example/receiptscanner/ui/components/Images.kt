package com.example.receiptscanner.ui.components

import android.content.Context
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject


import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember


import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import androidx.core.content.FileProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.util.Locale

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.content.ContextCompat

import java.io.FileNotFoundException
import java.io.IOException

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.receiptscanner.ui.screens.ApiKeyPreferences
import java.io.ByteArrayOutputStream

class AnthropicApiService(
    private val context: Context,
) {
    private val client = OkHttpClient()

    private suspend fun getCurrentApiKey(): String {
        return ApiKeyPreferences.getApiKey(context)
    }

    private fun compressImage(uri: Uri): String {
        // Get original bitmap
        val inputStream = context.contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)

        // Calculate new dimensions while maintaining aspect ratio
        val maxDimension = 1024 // Max width or height
        val ratio = minOf(
            maxDimension.toFloat() / originalBitmap.width,
            maxDimension.toFloat() / originalBitmap.height
        )

        val newWidth = (originalBitmap.width * ratio).toInt()
        val newHeight = (originalBitmap.height * ratio).toInt()

        // Create scaled bitmap
        val compressedBitmap = Bitmap.createScaledBitmap(
            originalBitmap,
            newWidth,
            newHeight,
            true
        )

        // Convert to base64
        val outputStream = ByteArrayOutputStream()
        compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()

        // Clean up
        if (originalBitmap != compressedBitmap) {
            originalBitmap.recycle()
        }
        compressedBitmap.recycle()
        outputStream.close()

        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun isAnthropicKey(apiKey: String): Boolean {
        return apiKey.startsWith("sk-ant")
    }

    private fun createAnthropicRequest(base64Image: String, apiKey: String): Request {
        val jsonBody = JSONObject().apply {
            put("model", "claude-3-5-sonnet-20241022")
            put("max_tokens", 4000)
            put("messages", JSONArray().put(
                JSONObject().apply {
                    put("role", "user")
                    put("content", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "image")
                            put("source", JSONObject().apply {
                                put("type", "base64")
                                put("media_type", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", """
                            You need to extract data from this receipt and return it as json
                            {
                                "description": "2-3 words description about this receipt, like the restaurant/business name, or the type of spending",
                                "totalAmount": Final grand total amount of the bill, int,
                                "addressLocation": "address of this merchant or restaurant",
                                "phoneNumberOfMerchant": "phone number of this merchant or business or anything",
                                "timestamp": "date and time of bill in 'DD/MM/YYYY HH:MM:SS' format",
                                "items": [
                                    {"some line item name": costInt}
                                ]
                             }
                             Keep field empty if not available but don't fill false info, Also, Please don't include any explanation, advices or any other text before and after json in your response.
                             Your response should only be json and will be parsed directly, please do not include any backticks or markdown like ```json, just directly output json
                             PLEASE DO NOT HALLUCINATE ANY INFO, ONLY WRITE WHAT IS THERE IN THE RECEIPT, IF NO RECEIPT, THEN RETURN EMPTY VALUES BUT RETURN KEYS
                            """.trimIndent())
                        })
                    })
                }
            ))
        }

        return Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()
    }

    private fun createOpenAIRequest(base64Image: String, apiKey: String): Request {
        val jsonBody = JSONObject().apply {
            put("model", "gpt-4o")
            put("messages", JSONArray().put(
                JSONObject().apply {
                    put("role", "user")
                    put("content", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", """
                            You need to extract data from this receipt and return it as json
                            {
                                "description": "2-3 words description about this receipt, like the restaurant/business name, or the type of spending",
                                "totalAmount": Final grand total amount of the bill, int,
                                "addressLocation": "address of this merchant or restaurant",
                                "phoneNumberOfMerchant": "phone number of this merchant or business or anything",
                                "timestamp": "date and time of bill in 'DD/MM/YYYY HH:MM:SS' format",
                                "items": [
                                    {"some line item name": costInt}
                                ]
                             }
                            Keep field empty if not available but don't fill false info, Also, Please don't include any explanation, advices or any other text before and after json in your response.
                            Your response should only be json and will be parsed directly, please do not include any backticks or markdown like ```json, just directly output json
                            your answer should start with opening curly brace "{" and end with closing curly brace "}"
                            PLEASE DO NOT HALLUCINATE ANY Receipt INFO, ONLY WRITE WHAT IS THERE IN THE RECEIPT, IF NO RECEIPT, THEN RETURN EMPTY VALUES BUT RETURN KEYS
                            """.trimIndent())
                        })
                        put(JSONObject().apply {
                            put("type", "image_url")
                            put("image_url", JSONObject().apply {
                                put("url", "data:image/jpeg;base64,$base64Image")
                            })
                        })
                    })
                }
            ))
            put("max_tokens", 4000)
        }

        return Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()
    }

    private fun parseResponse(responseBody: String, isAnthropicApi: Boolean): String {
        return if (isAnthropicApi) {
            val jsonResponse = JSONObject(responseBody)
            jsonResponse.getJSONArray("content").getJSONObject(0).getString("text")
        } else {
            val jsonResponse = JSONObject(responseBody)
            jsonResponse.getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content")
        }
    }

    suspend fun analyzeReceiptImage(imageUri: Uri, onProgress: (Float) -> Unit): String {
        return withContext(Dispatchers.IO) {
            try {
                onProgress(0.1f)
                val base64Image = compressImage(imageUri)
                onProgress(0.3f)

                val apiKey = getCurrentApiKey()
                if (apiKey.isBlank()) {
                    throw Exception("Please set your API key in settings")
                }

                val isAnthropicApi = isAnthropicKey(apiKey)
                val request = if (isAnthropicApi) {
                    createAnthropicRequest(base64Image, apiKey)
                } else {
                    createOpenAIRequest(base64Image, apiKey)
                }

                onProgress(0.7f)

                client.newCall(request).execute().use { response ->
                    onProgress(0.9f)

                    if (!response.isSuccessful) {
                        val errorMessage = "API call failed: ${response.code} ${response.message}"
                        println(errorMessage)

                        val responseBody = response.body?.string() ?: throw Exception("Empty response")
                        print(responseBody)

                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                        }
                        throw Exception(errorMessage)
                    }

                    val responseBody = response.body?.string() ?: throw Exception("Empty response")
                    onProgress(1.0f)
                    return@withContext parseResponse(responseBody, isAnthropicApi)
                }
            } catch (e: Exception) {
                handleError(e)
                onProgress(0f)
                return@withContext when (e) {
                    is OutOfMemoryError -> "Image is too large to process"
                    is SecurityException -> "Permission denied to access image"
                    is FileNotFoundException -> "Image file not found"
                    is IOException -> "Network error: ${e.message}"
                    else -> "Error analyzing receipt: ${e.message}"
                }
            }
        }
    }

    private suspend fun handleError(e: Exception) {
        withContext(Dispatchers.Main) {
            val errorMessage = when (e) {
                is OutOfMemoryError -> "Image is too large to process"
                is SecurityException -> "Permission denied to access image"
                is FileNotFoundException -> "Image file not found"
                is IOException -> "Network error: ${e.message}"
                else -> "Error analyzing receipt: ${e.message}"
            }
            println(errorMessage)
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePickerBottomSheet(
    show: Boolean,
    onDismiss: () -> Unit,
    onImageSelected: suspend (Uri) -> Unit
) {
    if (show) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val sheetState = rememberModalBottomSheetState()
        var imageUri by remember { mutableStateOf<Uri?>(null) }

        // Camera launcher
        val cameraLauncher = rememberLauncherForActivityResult(
            contract = TakePicture()
        ) { success ->
            if (success && imageUri != null) {
                scope.launch {
                    try {
                        onImageSelected(imageUri!!)
                        onDismiss()
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "Error processing image: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        // Permission launcher for camera
        val cameraPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                scope.launch {
                    try {
                        val uri = getImageUri(context)
                        imageUri = uri
                        cameraLauncher.launch(uri)
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "Error launching camera: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } else {
                Toast.makeText(
                    context,
                    "Camera permission is required to take photos",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Photo picker launcher
        val photoPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                scope.launch {
                    try {
                        onImageSelected(uri)
                        onDismiss()
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "Error processing image: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Select Image Source",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                ) {
                    Icon(Icons.Default.Photo, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Choose from Gallery")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        when (PackageManager.PERMISSION_GRANTED) {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) -> {
                                scope.launch {
                                    try {
                                        val uri = getImageUri(context)
                                        imageUri = uri
                                        cameraLauncher.launch(uri)
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            "Error launching camera: ${e.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                            else -> {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Take Photo")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private suspend fun getImageUri(context: Context): Uri {
    return withContext(Dispatchers.IO) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = context.cacheDir
        val tempFile = File.createTempFile(imageFileName, ".jpg", storageDir)
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    }
}