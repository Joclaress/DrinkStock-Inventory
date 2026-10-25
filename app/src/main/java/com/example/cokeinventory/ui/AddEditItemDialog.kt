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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.cokeinventory.model.CokeItem
import com.example.cokeinventory.ui.theme.CokeRed
import com.example.cokeinventory.util.ImageUtils
import com.example.cokeinventory.viewmodel.DEFAULT_EMPLOYEES

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditItemDialog(
    itemToEdit: CokeItem? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        category: String,
        stockQuantity: Int,
        costPrice: Double,
        sellingPrice: Double,
        employeeName: String,
        photoBase64: String?,
        unitsSold: Int
    ) -> Unit
) {
    var name by remember { mutableStateOf(itemToEdit?.name ?: "") }
    var category by remember { mutableStateOf(itemToEdit?.category ?: "Can") }
    var stockQuantityText by remember { mutableStateOf(itemToEdit?.stockQuantity?.toString() ?: "50") }
    var costPriceText by remember { mutableStateOf(itemToEdit?.costPrice?.toString() ?: "20.00") }
    var sellingPriceText by remember { mutableStateOf(itemToEdit?.sellingPrice?.toString() ?: "35.00") }
    var unitsSoldText by remember { mutableStateOf(itemToEdit?.unitsSold?.toString() ?: "0") }
    var selectedEmployee by remember { mutableStateOf(itemToEdit?.addedByEmployee ?: "Ngelo") }
    var photoBase64 by remember { mutableStateOf(itemToEdit?.photoBase64) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val categories = listOf("Can", "Bottle (500ml)", "Large Bottle (1.5L)", "Crate", "Other")
    var categoryExpanded by remember { mutableStateOf(false) }
    var employeeExpanded by remember { mutableStateOf(false) }

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
                errorMessage = "Unable to launch camera."
            }
        } else {
            errorMessage = "Camera permission is required to take photos."
        }
    }

    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                errorMessage = "Unable to launch camera."
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (itemToEdit == null) "Add New Softdrink Stock" else "Edit Softdrink Stock",
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
                // Photo Preview & Capture Bar
                val imageBitmap = remember(photoBase64) { ImageUtils.base64ToImageBitmap(photoBase64) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.LightGray.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageBitmap != null) {
                            Image(
                                bitmap = imageBitmap,
                                contentDescription = "Product Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text("No Photo", fontSize = 11.sp, color = Color.Gray)
                        }
                    }

                    Button(
                        onClick = { launchCamera() },
                        colors = ButtonDefaults.buttonColors(containerColor = CokeRed),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Take Photo", fontSize = 12.sp)
                    }
                }

                // Employee Selection Box (Ngelo, Epoy, Paul, Jonard, Ricky)
                ExposedDropdownMenuBox(
                    expanded = employeeExpanded,
                    onExpandedChange = { employeeExpanded = !employeeExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedEmployee,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Added by Employee") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = employeeExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = employeeExpanded,
                        onDismissRequest = { employeeExpanded = false }
                    ) {
                        DEFAULT_EMPLOYEES.forEach { emp ->
                            DropdownMenuItem(
                                text = { Text(emp, fontWeight = FontWeight.SemiBold) },
                                onClick = {
                                    selectedEmployee = emp
                                    employeeExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMessage = null },
                    label = { Text("Product Name (e.g. Sprite 500ml)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Dropdown Box
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = stockQuantityText,
                        onValueChange = { stockQuantityText = it },
                        label = { Text("Stock Quantity") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = costPriceText,
                        onValueChange = { costPriceText = it },
                        label = { Text("Cost Price (₱)") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = sellingPriceText,
                        onValueChange = { sellingPriceText = it },
                        label = { Text("Selling Price (₱)") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = unitsSoldText,
                        onValueChange = { unitsSoldText = it },
                        label = { Text("Units Sold") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                errorMessage?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Product name is required."
                        return@Button
                    }
                    val qty = stockQuantityText.toIntOrNull() ?: 0
                    val cost = costPriceText.toDoubleOrNull() ?: 0.0
                    val sell = sellingPriceText.toDoubleOrNull() ?: 0.0
                    val unitsSold = unitsSoldText.toIntOrNull() ?: 0

                    if (qty < 0 || cost < 0 || sell < 0 || unitsSold < 0) {
                        errorMessage = "Values must be non-negative numbers."
                        return@Button
                    }

                    onConfirm(name.trim(), category, qty, cost, sell, selectedEmployee, photoBase64, unitsSold)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CokeRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save Stock", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
