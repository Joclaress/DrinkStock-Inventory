package com.example.cokeinventory.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.cokeinventory.model.CokeItem
import com.example.cokeinventory.ui.theme.CokeRed
import com.example.cokeinventory.ui.theme.StockGreen
import com.example.cokeinventory.util.ImageUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellStockDialog(
    item: CokeItem,
    onDismiss: () -> Unit,
    onConfirmSale: (
        quantitySold: Int,
        customerName: String,
        photoBase64: String?
    ) -> Unit
) {
    var quantityText by remember { mutableStateOf("1") }
    var customerName by remember { mutableStateOf("") }
    var photoBase64 by remember { mutableStateOf<String?>(null) }

    val quantity = quantityText.toIntOrNull() ?: 0
    val totalRevenue = quantity * item.sellingPrice
    val totalProfit = quantity * item.profitPerUnit
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    // Camera Capture Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            photoBase64 = ImageUtils.bitmapToBase64(bitmap)
        }
    }

    // Camera Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                errorMsg = "Unable to launch camera."
            }
        } else {
            errorMsg = "Camera permission is required to take photos."
        }
    }

    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                errorMsg = "Unable to launch camera."
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Record Sale - ${item.name}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Current Stock: ${item.stockQuantity} available",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Notes Input
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Notes (e.g. Employee name, Customer, Store)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quantity Input
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = {
                        quantityText = it
                        errorMsg = null
                    },
                    label = { Text("Quantity Sold") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Proof Photo / Capture Section
                val imageBitmap = remember(photoBase64) { ImageUtils.base64ToImageBitmap(photoBase64) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.LightGray.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageBitmap != null) {
                            Image(
                                bitmap = imageBitmap,
                                contentDescription = "Sale Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text("No Proof", fontSize = 10.sp, color = Color.Gray)
                        }
                    }

                    Button(
                        onClick = { launchCamera() },
                        colors = ButtonDefaults.buttonColors(containerColor = CokeRed),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Take Photo", fontSize = 11.sp)
                    }
                }

                if (quantity > 0 && quantity <= item.stockQuantity) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Sale Revenue: ₱${String.format("%.2f", totalRevenue)}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Estimated Profit: ₱${String.format("%.2f", totalProfit)}",
                                fontWeight = FontWeight.Bold,
                                color = StockGreen,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                errorMsg?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (quantity <= 0) {
                        errorMsg = "Please enter a valid quantity."
                        return@Button
                    }
                    if (quantity > item.stockQuantity) {
                        errorMsg = "Cannot sell more than current stock (${item.stockQuantity})."
                        return@Button
                    }
                    onConfirmSale(
                        quantity,
                        customerName,
                        photoBase64
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = CokeRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Confirm Sale", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
