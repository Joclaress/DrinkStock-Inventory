package com.example.cokeinventory.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.cokeinventory.model.CokeItem
import com.example.cokeinventory.model.SaleTransaction
import com.example.cokeinventory.ui.theme.*
import com.example.cokeinventory.util.AdMobManager
import com.example.cokeinventory.util.BannerAdView
import com.example.cokeinventory.util.ImageUtils
import com.google.android.gms.ads.rewarded.RewardedAd
import com.example.cokeinventory.viewmodel.DEFAULT_EMPLOYEES
import com.example.cokeinventory.viewmodel.InventoryUiState
import com.example.cokeinventory.viewmodel.InventoryViewModel
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private sealed interface PendingAdminAction {
    object AddNewStock : PendingAdminAction
    data class EditStock(val item: CokeItem) : PendingAdminAction
    data class AddQuickStock(val item: CokeItem) : PendingAdminAction
    data class DeleteStock(val item: CokeItem) : PendingAdminAction
    object ResetSales : PendingAdminAction
    data class EditSaleTransaction(val transaction: SaleTransaction) : PendingAdminAction
}

private enum class SalesFilter(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    TOTAL("Total")
}

private enum class BottomNavTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    ADD_STOCK("Add Stock", Icons.Default.AddCircleOutline),
    SALES_REPORT("Sales Report", Icons.Default.BarChart),
    PROFILE("Profile", Icons.Default.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: InventoryViewModel,
    uiState: InventoryUiState,
    adminPin: String = "admin123",
    onUpdateAdminPin: ((String) -> Unit)? = null,
    onReplayTutorial: (() -> Unit)? = null
) {
    var currentTab by remember { mutableStateOf(BottomNavTab.HOME) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showSalesHistoryDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<CokeItem?>(null) }
    var itemToSell by remember { mutableStateOf<CokeItem?>(null) }
    var transactionToEdit by remember { mutableStateOf<SaleTransaction?>(null) }
    var pendingAdminAction by remember { mutableStateOf<PendingAdminAction?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    val filteredItems = remember(uiState.items, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.items
        } else {
            uiState.items.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.category.contains(searchQuery, ignoreCase = true) ||
                        it.addedByEmployee.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                BottomNavTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (tab == BottomNavTab.ADD_STOCK) {
                                pendingAdminAction = PendingAdminAction.AddNewStock
                            } else {
                                currentTab = tab
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DrinkStockPink,
                            selectedTextColor = DrinkStockPink,
                            indicatorColor = CokeRedBg,
                            unselectedIconColor = CokeGray,
                            unselectedTextColor = CokeGray
                        )
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Floating Top Header Section (Replaces TopAppBar)
            TopHeaderSection(
                uiState = uiState,
                onSignOutClick = { viewModel.signOut() }
            )

            when (currentTab) {
                BottomNavTab.HOME, BottomNavTab.ADD_STOCK -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        // Metrics Section
                        MetricsSection(uiState)

                        // Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search Softdrink products or employee...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CokeGray) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Inventory Item List
                        if (filteredItems.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = CokeGray
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (searchQuery.isNotBlank()) "No Softdrinks match '$searchQuery'" else "No stock items in inventory",
                                        fontSize = 16.sp,
                                        color = CokeGray,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filteredItems, key = { it.id.ifEmpty { it.name } }) { item ->
                                    val itemSalesUnits = remember(uiState.salesTransactions, item) {
                                        uiState.salesTransactions.filter { it.itemId == item.id || (it.itemId.isBlank() && it.itemName == item.name) }.sumOf { it.quantitySold }
                                    }
                                    val effectiveItem = remember(item, itemSalesUnits) {
                                        if (itemSalesUnits > item.unitsSold) {
                                            item.copy(unitsSold = itemSalesUnits)
                                        } else {
                                            item
                                        }
                                    }

                                    CokeItemCard(
                                        item = effectiveItem,
                                        onSellClick = { itemToSell = effectiveItem },
                                        onAddStock = { pendingAdminAction = PendingAdminAction.AddQuickStock(effectiveItem) },
                                        onEditClick = { pendingAdminAction = PendingAdminAction.EditStock(effectiveItem) },
                                        onDeleteClick = { pendingAdminAction = PendingAdminAction.DeleteStock(effectiveItem) }
                                    )
                                }
                                item {
                                    Column {
                                        BannerAdView(modifier = Modifier.padding(vertical = 8.dp))
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                BottomNavTab.SALES_REPORT -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        SalesReportScreenContent(
                            transactions = uiState.salesTransactions,
                            onResetSalesClick = { pendingAdminAction = PendingAdminAction.ResetSales },
                            onEditSaleClick = { pendingAdminAction = PendingAdminAction.EditSaleTransaction(it) }
                        )
                    }
                }
                BottomNavTab.PROFILE -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        ProfileScreenContent(
                            uiState = uiState,
                            adminPin = adminPin,
                            onUpdateAdminPin = onUpdateAdminPin,
                            onSignOutClick = { viewModel.signOut() },
                            onReplayTutorial = onReplayTutorial
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Dialog
    if (showAddDialog) {
        AddEditItemDialog(
            itemToEdit = itemToEdit,
            onDismiss = {
                showAddDialog = false
                itemToEdit = null
            },
            onConfirm = { name, category, qty, cost, sell, employeeName, photoBase64, unitsSold ->
                viewModel.addOrUpdateCokeItem(
                    id = itemToEdit?.id,
                    name = name,
                    category = category,
                    stockQuantity = qty,
                    costPrice = cost,
                    sellingPrice = sell,
                    addedByEmployee = employeeName,
                    photoBase64 = photoBase64,
                    unitsSold = unitsSold
                )
                showAddDialog = false
                itemToEdit = null
            }
        )
    }

    // Sell Dialog
    itemToSell?.let { item ->
        SellStockDialog(
            item = item,
            onDismiss = { itemToSell = null },
            onConfirmSale = { qty, customerName, photoBase64 ->
                viewModel.recordSale(
                    item = item,
                    quantitySold = qty,
                    customerName = customerName,
                    photoBase64 = photoBase64
                )
                itemToSell = null
            }
        )
    }

    // Sales History Log Dialog
    if (showSalesHistoryDialog) {
        SalesHistoryDialog(
            transactions = uiState.salesTransactions,
            onResetSalesClick = {
                showSalesHistoryDialog = false
                pendingAdminAction = PendingAdminAction.ResetSales
            },
            onEditSaleClick = { tx ->
                showSalesHistoryDialog = false
                pendingAdminAction = PendingAdminAction.EditSaleTransaction(tx)
            },
            onDismiss = { showSalesHistoryDialog = false }
        )
    }

    // Admin Password Verification Dialog
    pendingAdminAction?.let { action ->
        AdminPasswordDialog(
            adminPin = adminPin,
            onDismiss = { pendingAdminAction = null },
            onSuccess = {
                when (action) {
                    is PendingAdminAction.AddNewStock -> {
                        itemToEdit = null
                        showAddDialog = true
                    }
                    is PendingAdminAction.EditStock -> {
                        itemToEdit = action.item
                        showAddDialog = true
                    }
                    is PendingAdminAction.AddQuickStock -> {
                        viewModel.addStock(action.item, 12)
                    }
                    is PendingAdminAction.DeleteStock -> {
                        viewModel.deleteCokeItem(action.item.id)
                    }
                    is PendingAdminAction.ResetSales -> {
                        viewModel.resetAllSalesAndProfits()
                    }
                    is PendingAdminAction.EditSaleTransaction -> {
                        transactionToEdit = action.transaction
                    }
                }
                pendingAdminAction = null
            }
        )
    }

    // Edit Sale Transaction Dialog
    transactionToEdit?.let { tx ->
        EditSaleTransactionDialog(
            transaction = tx,
            onDismiss = { transactionToEdit = null },
            onConfirm = { name, qty, rev, prof, employee, customer, photoBase64 ->
                viewModel.updateSaleTransaction(
                    transactionId = tx.id,
                    newItemName = name,
                    newQuantitySold = qty,
                    newTotalRevenue = rev,
                    newProfitMade = prof,
                    newEmployeeName = employee,
                    newCustomerName = customer,
                    newPhotoBase64 = photoBase64
                )
                transactionToEdit = null
            }
        )
    }
}

@Composable
private fun TopHeaderSection(
    uiState: InventoryUiState,
    onSignOutClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DrinkStockHeader(
                iconSize = 40.dp,
                titleColor = MaterialTheme.colorScheme.onSurface,
                isCentered = false
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE8F5E9)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                        )
                        Text(
                            text = "Active",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }

                IconButton(
                    onClick = onSignOutClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Sign Out",
                        tint = CokeGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricsSection(uiState: InventoryUiState) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            MetricCard(
                title = "TOTAL STOCKS",
                value = "${uiState.totalStockCount} units",
                subtitle = "${uiState.items.size} product lines",
                icon = Icons.Default.ShoppingCart,
                accentColor = CokeRed,
                bgColor = CokeRedBg
            )
        }
        item {
            MetricCard(
                title = "NET PROFIT",
                value = "₱${String.format(Locale.US, "%.2f", uiState.totalProfit)}",
                subtitle = "${uiState.totalUnitsSold} total units sold",
                icon = Icons.Default.Star,
                accentColor = StockGreen,
                bgColor = StockGreenBg
            )
        }
        item {
            MetricCard(
                title = "TOTAL REVENUE",
                value = "₱${String.format(Locale.US, "%.2f", uiState.totalRevenue)}",
                subtitle = "Gross sales amount",
                icon = Icons.Default.Star,
                accentColor = CokeDarkRed,
                bgColor = CokeRedBg
            )
        }
        item {
            MetricCard(
                title = "STOCK VALUATION",
                value = "₱${String.format(Locale.US, "%.2f", uiState.totalStockValue)}",
                subtitle = "Total cost of stock",
                icon = Icons.Default.ShoppingCart,
                accentColor = CokeBlack,
                bgColor = CokeLightGray
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    bgColor: Color
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.width(180.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    letterSpacing = 0.8.sp
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CokeItemCard(
    item: CokeItem,
    onSellClick: () -> Unit,
    onAddStock: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val photoBitmap = remember(item.photoBase64) { ImageUtils.base64ToImageBitmap(item.photoBase64) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CokeRed),
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoBitmap != null) {
                            Image(
                                bitmap = photoBitmap,
                                contentDescription = item.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = "SODA",
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = item.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = CokeLightGray,
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Text(
                                    text = item.category,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            // Low Stock Warning Badge
                            val isLowStock = item.stockQuantity < 15
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isLowStock) StockWarningBg else StockGreenBg
                            ) {
                                Text(
                                    text = if (isLowStock) "Low Stock (${item.stockQuantity})" else "${item.stockQuantity} in stock",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLowStock) StockWarning else StockGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Added By Employee Tag
                        Text(
                            text = "Added by: ${item.addedByEmployee}",
                            fontSize = 11.sp,
                            color = CokeGray,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = CokeGray, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CokeRed, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = CokeLightGray, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Pricing & Profit Details Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Cost Price", fontSize = 11.sp, color = CokeGray)
                    Text(text = "₱${String.format(Locale.US, "%.2f", item.costPrice)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Column {
                    Text(text = "Selling Price", fontSize = 11.sp, color = CokeGray)
                    Text(text = "₱${String.format(Locale.US, "%.2f", item.sellingPrice)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Column {
                    Text(text = "Profit / Unit", fontSize = 11.sp, color = CokeGray)
                    Text(
                        text = "+₱${String.format(Locale.US, "%.2f", item.profitPerUnit)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = StockGreen
                    )
                }

                Column {
                    Text(text = "Total Earned Profit", fontSize = 11.sp, color = CokeGray)
                    Text(
                        text = "₱${String.format(Locale.US, "%.2f", item.totalProfit)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = StockGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons (Record Sale / Restock)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSellClick,
                    enabled = item.stockQuantity > 0,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = CokeRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Record Sale", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onAddStock,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "+12 Stock", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SalesHistoryDialog(
    transactions: List<SaleTransaction>,
    onResetSalesClick: () -> Unit,
    onEditSaleClick: (SaleTransaction) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf(SalesFilter.TODAY) }
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val dateTimeFormat = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }

    fun isSameDay(time1: Long, time2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun isYesterday(timestamp: Long): Boolean {
        val calTx = Calendar.getInstance().apply { timeInMillis = timestamp }
        val calYesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        return calTx.get(Calendar.YEAR) == calYesterday.get(Calendar.YEAR) &&
               calTx.get(Calendar.DAY_OF_YEAR) == calYesterday.get(Calendar.DAY_OF_YEAR)
    }

    fun isThisWeek(timestamp: Long): Boolean {
        val calTx = Calendar.getInstance().apply { timeInMillis = timestamp }
        val calNow = Calendar.getInstance()
        return calTx.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
               calTx.get(Calendar.WEEK_OF_YEAR) == calNow.get(Calendar.WEEK_OF_YEAR)
    }

    fun isThisMonth(timestamp: Long): Boolean {
        val calTx = Calendar.getInstance().apply { timeInMillis = timestamp }
        val calNow = Calendar.getInstance()
        return calTx.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
               calTx.get(Calendar.MONTH) == calNow.get(Calendar.MONTH)
    }

    val now = System.currentTimeMillis()

    val todaySales = remember(transactions) { transactions.filter { isSameDay(it.timestamp, now) } }
    val todayRevenue = remember(todaySales) { todaySales.sumOf { it.totalRevenue } }
    val todayProfit = remember(todaySales) { todaySales.sumOf { it.profitMade } }

    val yesterdaySales = remember(transactions) { transactions.filter { isYesterday(it.timestamp) } }
    val yesterdayRevenue = remember(yesterdaySales) { yesterdaySales.sumOf { it.totalRevenue } }
    val yesterdayProfit = remember(yesterdaySales) { yesterdaySales.sumOf { it.profitMade } }

    val weekSales = remember(transactions) { transactions.filter { isThisWeek(it.timestamp) } }
    val weekRevenue = remember(weekSales) { weekSales.sumOf { it.totalRevenue } }
    val weekProfit = remember(weekSales) { weekSales.sumOf { it.profitMade } }

    val monthSales = remember(transactions) { transactions.filter { isThisMonth(it.timestamp) } }
    val monthRevenue = remember(monthSales) { monthSales.sumOf { it.totalRevenue } }
    val monthProfit = remember(monthSales) { monthSales.sumOf { it.profitMade } }

    val totalRevenue = remember(transactions) { transactions.sumOf { it.totalRevenue } }
    val totalProfit = remember(transactions) { transactions.sumOf { it.profitMade } }

    val filteredTransactions = remember(transactions, selectedFilter) {
        when (selectedFilter) {
            SalesFilter.TODAY -> todaySales
            SalesFilter.YESTERDAY -> yesterdaySales
            SalesFilter.THIS_WEEK -> weekSales
            SalesFilter.THIS_MONTH -> monthSales
            SalesFilter.TOTAL -> transactions
        }
    }

    fun getSectionCategory(timestamp: Long): String {
        return when {
            isSameDay(timestamp, now) -> "Today's Sales"
            isYesterday(timestamp) -> "Yesterday's Sales"
            isThisWeek(timestamp) -> "This Week's Sales"
            isThisMonth(timestamp) -> "This Month's Sales"
            else -> "Older Sales"
        }
    }

    val sectionOrder = listOf("Today's Sales", "Yesterday's Sales", "This Week's Sales", "This Month's Sales", "Older Sales")

    val groupedTransactions = remember(filteredTransactions) {
        val grouped = filteredTransactions.groupBy { getSectionCategory(it.timestamp) }
        sectionOrder.mapNotNull { category ->
            grouped[category]?.let { list -> category to list }
        }
    }

    fun formatTxDate(timestamp: Long): String {
        return when {
            isSameDay(timestamp, now) -> timeFormat.format(Date(timestamp))
            isYesterday(timestamp) -> "Yesterday, ${timeFormat.format(Date(timestamp))}"
            else -> dateTimeFormat.format(Date(timestamp))
        }
    }

    val filterCardsData = remember(todayRevenue, todayProfit, yesterdayRevenue, yesterdayProfit, weekRevenue, weekProfit, monthRevenue, monthProfit, totalRevenue, totalProfit) {
        listOf(
            SalesFilter.TODAY to (todayRevenue to todayProfit),
            SalesFilter.YESTERDAY to (yesterdayRevenue to yesterdayProfit),
            SalesFilter.THIS_WEEK to (weekRevenue to weekProfit),
            SalesFilter.THIS_MONTH to (monthRevenue to monthProfit),
            SalesFilter.TOTAL to (totalRevenue to totalProfit)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = CokeRed)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Sales Log", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }

                TextButton(
                    onClick = onResetSalesClick,
                    colors = ButtonDefaults.textButtonColors(contentColor = CokeRed),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Reset Sales", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Reset Sales", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        text = {
            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No sales recorded yet.",
                        color = CokeGray,
                        fontSize = 14.sp
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Clickable Filter Cards (Today, Yesterday, This Week, This Month, Total)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filterCardsData, key = { it.first.name }) { (filter, revAndProfit) ->
                            val (rev, prof) = revAndProfit
                            val isSelected = selectedFilter == filter

                            Card(
                                onClick = { selectedFilter = filter },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) CokeRed else CokeLightGray
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.width(105.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = filter.label.uppercase(Locale.ROOT),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White.copy(alpha = 0.9f) else CokeRed
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "₱${String.format(Locale.US, "%.2f", rev)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "+₱${String.format(Locale.US, "%.2f", prof)}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) Color(0xFFA5D6A7) else StockGreen
                                    )
                                }
                            }
                        }
                    }

                    // Scrollable Filtered Transaction History
                    if (filteredTransactions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No sales recorded for ${selectedFilter.label.lowercase()}.",
                                color = CokeGray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 360.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            groupedTransactions.forEach { (dateHeader, txList) ->
                                val sectionRevenue = txList.sumOf { it.totalRevenue }
                                val sectionProfit = txList.sumOf { it.profitMade }

                                item(key = "header_${selectedFilter.name}_$dateHeader") {
                                    Column(modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = dateHeader,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = CokeRed
                                            )
                                            Text(
                                                text = "₱${String.format(Locale.US, "%.2f", sectionRevenue)} (+₱${String.format(Locale.US, "%.2f", sectionProfit)})",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        HorizontalDivider(
                                            modifier = Modifier.padding(top = 2.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                }

                                items(txList, key = { it.id.ifEmpty { it.timestamp.toString() } }) { tx ->
                                    val txPhoto = remember(tx.photoBase64) { ImageUtils.base64ToImageBitmap(tx.photoBase64) }

                                    Card(
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = CokeLightGray),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            if (txPhoto != null) {
                                                Image(
                                                    bitmap = txPhoto,
                                                    contentDescription = "Proof",
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(RoundedCornerShape(8.dp)),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(CokeRed.copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Person, contentDescription = null, tint = CokeRed)
                                                }
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = tx.itemName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                                if (tx.customerName.isNotBlank() && tx.customerName != "Walk-in Customer") {
                                                    Text(
                                                        text = "Note: ${tx.customerName}",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                Text(
                                                    text = "${tx.quantitySold} unit(s) • ${formatTxDate(tx.timestamp)}",
                                                    fontSize = 11.sp,
                                                    color = CokeGray
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = "₱${String.format(Locale.US, "%.2f", tx.totalRevenue)}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    )
                                                    Text(
                                                        text = "+₱${String.format(Locale.US, "%.2f", tx.profitMade)} profit",
                                                        fontWeight = FontWeight.Bold,
                                                        color = StockGreen,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { onEditSaleClick(tx) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit Sale",
                                                        tint = CokeGray,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun AdminPasswordDialog(
    adminPin: String = "admin123",
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = CokeRed
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Admin Access Required", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Please enter the admin password to perform this action.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordError = false
                    },
                    label = { Text("Admin Password") },
                    singleLine = true,
                    isError = passwordError,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                if (passwordError) {
                    Text(
                        text = "Incorrect admin password!",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (password == adminPin) {
                        onSuccess()
                    } else {
                        passwordError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CokeRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Confirm", color = Color.White)
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
private fun SalesReportScreenContent(
    transactions: List<SaleTransaction>,
    onResetSalesClick: () -> Unit,
    onEditSaleClick: (SaleTransaction) -> Unit
) {
    var unlockedFilters by remember { mutableStateOf(setOf<SalesFilter>()) }
    var selectedFilter by remember { mutableStateOf(SalesFilter.TOTAL) }
    var pendingFilterToUnlock by remember { mutableStateOf<SalesFilter?>(null) }
    var isLoadingAd by remember { mutableStateOf(false) }
    var adError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val activity = context as? Activity
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val dateTimeFormat = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }

    fun isSameDay(time1: Long, time2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun isYesterday(timestamp: Long): Boolean {
        val calTx = Calendar.getInstance().apply { timeInMillis = timestamp }
        val calYesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        return calTx.get(Calendar.YEAR) == calYesterday.get(Calendar.YEAR) &&
               calTx.get(Calendar.DAY_OF_YEAR) == calYesterday.get(Calendar.DAY_OF_YEAR)
    }

    fun isThisWeek(timestamp: Long): Boolean {
        val calTx = Calendar.getInstance().apply { timeInMillis = timestamp }
        val calNow = Calendar.getInstance()
        return calTx.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
               calTx.get(Calendar.WEEK_OF_YEAR) == calNow.get(Calendar.WEEK_OF_YEAR)
    }

    fun isThisMonth(timestamp: Long): Boolean {
        val calTx = Calendar.getInstance().apply { timeInMillis = timestamp }
        val calNow = Calendar.getInstance()
        return calTx.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
               calTx.get(Calendar.MONTH) == calNow.get(Calendar.MONTH)
    }

    val now = System.currentTimeMillis()

    val todaySales = remember(transactions) { transactions.filter { isSameDay(it.timestamp, now) } }
    val todayRevenue = remember(todaySales) { todaySales.sumOf { it.totalRevenue } }
    val todayProfit = remember(todaySales) { todaySales.sumOf { it.profitMade } }

    val yesterdaySales = remember(transactions) { transactions.filter { isYesterday(it.timestamp) } }
    val yesterdayRevenue = remember(yesterdaySales) { yesterdaySales.sumOf { it.totalRevenue } }
    val yesterdayProfit = remember(yesterdaySales) { yesterdaySales.sumOf { it.profitMade } }

    val weekSales = remember(transactions) { transactions.filter { isThisWeek(it.timestamp) } }
    val weekRevenue = remember(weekSales) { weekSales.sumOf { it.totalRevenue } }
    val weekProfit = remember(weekSales) { weekSales.sumOf { it.profitMade } }

    val monthSales = remember(transactions) { transactions.filter { isThisMonth(it.timestamp) } }
    val monthRevenue = remember(monthSales) { monthSales.sumOf { it.totalRevenue } }
    val monthProfit = remember(monthSales) { monthSales.sumOf { it.profitMade } }

    val totalRevenue = remember(transactions) { transactions.sumOf { it.totalRevenue } }
    val totalProfit = remember(transactions) { transactions.sumOf { it.profitMade } }

    val filteredTransactions = remember(transactions, selectedFilter) {
        when (selectedFilter) {
            SalesFilter.TODAY -> todaySales
            SalesFilter.YESTERDAY -> yesterdaySales
            SalesFilter.THIS_WEEK -> weekSales
            SalesFilter.THIS_MONTH -> monthSales
            SalesFilter.TOTAL -> transactions
        }
    }

    fun getSectionCategory(timestamp: Long): String {
        return when {
            isSameDay(timestamp, now) -> "Today's Sales"
            isYesterday(timestamp) -> "Yesterday's Sales"
            isThisWeek(timestamp) -> "This Week's Sales"
            isThisMonth(timestamp) -> "This Month's Sales"
            else -> "Older Sales"
        }
    }

    val sectionOrder = listOf("Today's Sales", "Yesterday's Sales", "This Week's Sales", "This Month's Sales", "Older Sales")

    val groupedTransactions = remember(filteredTransactions) {
        val grouped = filteredTransactions.groupBy { getSectionCategory(it.timestamp) }
        sectionOrder.mapNotNull { category ->
            grouped[category]?.let { list -> category to list }
        }
    }

    fun formatTxDate(timestamp: Long): String {
        return when {
            isSameDay(timestamp, now) -> timeFormat.format(Date(timestamp))
            isYesterday(timestamp) -> "Yesterday, ${timeFormat.format(Date(timestamp))}"
            else -> dateTimeFormat.format(Date(timestamp))
        }
    }

    val filterCardsData = remember(todayRevenue, todayProfit, yesterdayRevenue, yesterdayProfit, weekRevenue, weekProfit, monthRevenue, monthProfit, totalRevenue, totalProfit) {
        listOf(
            SalesFilter.TODAY to (todayRevenue to todayProfit),
            SalesFilter.YESTERDAY to (yesterdayRevenue to yesterdayProfit),
            SalesFilter.THIS_WEEK to (weekRevenue to weekProfit),
            SalesFilter.THIS_MONTH to (monthRevenue to monthProfit),
            SalesFilter.TOTAL to (totalRevenue to totalProfit)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Sales & Profit Report",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${transactions.size} total sales recorded",
                    fontSize = 12.sp,
                    color = CokeGray
                )
            }

            TextButton(
                onClick = onResetSalesClick,
                colors = ButtonDefaults.textButtonColors(contentColor = CokeRed)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset Sales", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filterCardsData, key = { it.first.name }) { (filter, revAndProfit) ->
                val (rev, prof) = revAndProfit
                val isSelected = selectedFilter == filter

                val isUnlocked = filter in unlockedFilters

                Card(
                    onClick = {
                        if (!isUnlocked) {
                            pendingFilterToUnlock = filter
                        } else {
                            selectedFilter = filter
                        }
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) CokeRed else CokeLightGray
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.width(110.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = filter.label.uppercase(Locale.ROOT),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White.copy(alpha = 0.9f) else CokeRed
                            )
                            if (!isUnlocked) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = if (isSelected) Color.White else CokeRed,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isUnlocked) "₱${String.format(Locale.US, "%.2f", rev)}" else "₱•••.••",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isUnlocked) "+₱${String.format(Locale.US, "%.2f", prof)}" else "+₱🔒.🔒🔒",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) Color(0xFFA5D6A7) else StockGreen
                        )
                    }
                }
            }
        }

        val isCurrentFilterUnlocked = selectedFilter in unlockedFilters

        if (!isCurrentFilterUnlocked) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(CokeRed.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = CokeRed,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "🔒 ${selectedFilter.label} Sales Data Encrypted",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Watch a short video ad to decrypt and reveal ${selectedFilter.label}'s revenue, profit figures, and complete transaction log.",
                        fontSize = 13.sp,
                        color = CokeGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { pendingFilterToUnlock = selectedFilter },
                        colors = ButtonDefaults.buttonColors(containerColor = DrinkStockPink),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Watch Video Ad to Decrypt Sales",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }
            }
        } else if (filteredTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No sales recorded for ${selectedFilter.label.lowercase()}.",
                    color = CokeGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                groupedTransactions.forEach { (dateHeader, txList) ->
                    val sectionRevenue = txList.sumOf { it.totalRevenue }
                    val sectionProfit = txList.sumOf { it.profitMade }

                    item(key = "hdr_${selectedFilter.name}_$dateHeader") {
                        Column(modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dateHeader,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = CokeRed
                                )
                                Text(
                                    text = "₱${String.format(Locale.US, "%.2f", sectionRevenue)} (+₱${String.format(Locale.US, "%.2f", sectionProfit)})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(top = 2.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }

                    items(txList, key = { it.id.ifEmpty { it.timestamp.toString() } }) { tx ->
                        val txPhoto = remember(tx.photoBase64) { ImageUtils.base64ToImageBitmap(tx.photoBase64) }

                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = CokeLightGray),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (txPhoto != null) {
                                    Image(
                                        bitmap = txPhoto,
                                        contentDescription = "Proof",
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(CokeRed.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = CokeRed)
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tx.itemName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    if (tx.customerName.isNotBlank() && tx.customerName != "Walk-in Customer") {
                                        Text(
                                            text = "Note: ${tx.customerName}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = "${tx.quantitySold} unit(s) • ${formatTxDate(tx.timestamp)}",
                                        fontSize = 11.sp,
                                        color = CokeGray
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "₱${String.format(Locale.US, "%.2f", tx.totalRevenue)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "+₱${String.format(Locale.US, "%.2f", tx.profitMade)} profit",
                                            fontWeight = FontWeight.Bold,
                                            color = StockGreen,
                                            fontSize = 11.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = { onEditSaleClick(tx) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Sale",
                                            tint = CokeGray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            BannerAdView(modifier = Modifier.padding(top = 8.dp))
        }

        pendingFilterToUnlock?.let { targetFilter ->
            AlertDialog(
                onDismissRequest = {
                    if (!isLoadingAd) pendingFilterToUnlock = null
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = DrinkStockPink,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Watch Video Ad to Unlock ${targetFilter.label}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Watch a short video ad to unlock and view the ${targetFilter.label} detailed sales and profit report.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        adError?.let { err ->
                            Text(text = err, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (activity == null) return@Button
                            isLoadingAd = true
                            adError = null

                            AdMobManager.loadRewardedAd(
                                context = context,
                                onAdLoaded = { rewardedAd ->
                                    isLoadingAd = false
                                    AdMobManager.showRewardedAd(
                                        activity = activity,
                                        rewardedAd = rewardedAd,
                                        onUserRewarded = {
                                            unlockedFilters = unlockedFilters + targetFilter
                                            selectedFilter = targetFilter
                                            pendingFilterToUnlock = null
                                            Toast.makeText(context, "${targetFilter.label} report unlocked!", Toast.LENGTH_SHORT).show()
                                        },
                                        onAdDismissed = {
                                            isLoadingAd = false
                                        },
                                        onAdFailedToShow = { err ->
                                            isLoadingAd = false
                                            adError = "Ad error ($err). Try again."
                                        }
                                    )
                                },
                                onAdFailed = { err ->
                                    isLoadingAd = false
                                    adError = "Ad unavailable ($err). Please try again later."
                                }
                            )
                        },
                        enabled = !isLoadingAd,
                        colors = ButtonDefaults.buttonColors(containerColor = DrinkStockPink),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isLoadingAd) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Loading Ad...", fontSize = 13.sp)
                        } else {
                            Text("Watch Video Ad & Unlock", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { pendingFilterToUnlock = null },
                        enabled = !isLoadingAd
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun ProfileScreenContent(
    uiState: InventoryUiState,
    adminPin: String = "admin123",
    onUpdateAdminPin: ((String) -> Unit)? = null,
    onSignOutClick: () -> Unit,
    onReplayTutorial: (() -> Unit)? = null
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var showChangePinDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(CokeRed.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Profile",
                        tint = CokeRed,
                        modifier = Modifier.size(64.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (uiState.isGuestMode) "Guest User / Demo Mode" else (uiState.currentUser?.email ?: "Manager Account"),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = if (uiState.isGuestMode) "Temporary Demo Session" else "Store Manager & Admin Access",
                    fontSize = 12.sp,
                    color = CokeGray
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StockGreen
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Inventory Cloud Sync Active",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CokeLightGray)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Inventory Overview",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Products:", fontSize = 13.sp, color = CokeGray)
                    Text("${uiState.items.size} items", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Units in Stock:", fontSize = 13.sp, color = CokeGray)
                    Text("${uiState.totalStockCount} units", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Earned Profit:", fontSize = 13.sp, color = CokeGray)
                    Text("₱${String.format(Locale.US, "%.2f", uiState.totalProfit)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = StockGreen)
                }
            }
        }

        // Rewarded Video Ad Section
        RewardedVideoAdCard()

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = StockGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Account & Security", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Text(
                    text = "Admin Security: Active • PIN Protected",
                    fontSize = 12.sp,
                    color = CokeGray
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                OutlinedButton(
                    onClick = { showChangePinDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = StockGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Change Admin Security PIN",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                onReplayTutorial?.let { replay ->
                    OutlinedButton(
                        onClick = replay,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Replay App Tutorial Walkthrough",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Button(
                    onClick = onSignOutClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DrinkStockPink),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Log Out"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Log Out Account",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }
        }

        if (showChangePinDialog) {
            ChangeAdminPinDialog(
                currentAdminPin = adminPin,
                onDismiss = { showChangePinDialog = false },
                onPinChanged = { newPin ->
                    onUpdateAdminPin?.invoke(newPin)
                    showChangePinDialog = false
                    Toast.makeText(context, "Admin PIN updated successfully!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ChangeAdminPinDialog(
    currentAdminPin: String,
    onDismiss: () -> Unit,
    onPinChanged: (String) -> Unit
) {
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = StockGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Change Admin PIN", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Update your admin PIN to secure stock restocking, sales resets, and product edits.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = oldPin,
                    onValueChange = { oldPin = it; errorMessage = null },
                    label = { Text("Current Admin PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newPin,
                    onValueChange = { newPin = it; errorMessage = null },
                    label = { Text("New Admin PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { confirmPin = it; errorMessage = null },
                    label = { Text("Confirm New Admin PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )

                errorMessage?.let { err ->
                    Text(text = err, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (oldPin != currentAdminPin) {
                        errorMessage = "Current Admin PIN is incorrect!"
                    } else if (newPin.isBlank() || newPin.length < 4) {
                        errorMessage = "New PIN must be at least 4 characters."
                    } else if (newPin != confirmPin) {
                        errorMessage = "New PINs do not match!"
                    } else {
                        onPinChanged(newPin)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = StockGreen),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save New PIN", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSaleTransactionDialog(
    transaction: SaleTransaction,
    onDismiss: () -> Unit,
    onConfirm: (
        itemName: String,
        quantitySold: Int,
        totalRevenue: Double,
        profitMade: Double,
        employeeName: String,
        customerName: String,
        photoBase64: String?
    ) -> Unit
) {
    var itemName by remember { mutableStateOf(transaction.itemName) }
    var quantityText by remember { mutableStateOf(transaction.quantitySold.toString()) }
    var revenueText by remember { mutableStateOf(transaction.totalRevenue.toString()) }
    var profitText by remember { mutableStateOf(transaction.profitMade.toString()) }
    var selectedEmployee by remember { mutableStateOf(transaction.employeeName) }
    var customerName by remember { mutableStateOf(transaction.customerName) }
    var photoBase64 by remember { mutableStateOf(transaction.photoBase64) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var employeeExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            photoBase64 = ImageUtils.bitmapToBase64(bitmap)
        }
    }

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
            errorMessage = "Camera permission is required."
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
            Text(text = "Edit Sale Details", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val imageBitmap = remember(photoBase64) { ImageUtils.base64ToImageBitmap(photoBase64) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                                contentDescription = "Proof",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text("No Proof", fontSize = 10.sp, color = Color.Gray)
                        }
                    }

                    Button(
                        onClick = { launchCamera() },
                        colors = ButtonDefaults.buttonColors(containerColor = DrinkStockPink),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Update Photo", fontSize = 12.sp)
                    }
                }

                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Product Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("Quantity Sold") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = revenueText,
                    onValueChange = { revenueText = it },
                    label = { Text("Total Revenue (₱)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = profitText,
                    onValueChange = { profitText = it },
                    label = { Text("Profit Made (₱)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                ExposedDropdownMenuBox(
                    expanded = employeeExpanded,
                    onExpandedChange = { employeeExpanded = !employeeExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedEmployee,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sold by Employee") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = employeeExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = employeeExpanded,
                        onDismissRequest = { employeeExpanded = false }
                    ) {
                        DEFAULT_EMPLOYEES.forEach { emp ->
                            DropdownMenuItem(
                                text = { Text(emp) },
                                onClick = {
                                    selectedEmployee = emp
                                    employeeExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Customer Note / Info") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                errorMessage?.let { err ->
                    Text(text = err, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantityText.toIntOrNull() ?: 0
                    val rev = revenueText.toDoubleOrNull() ?: 0.0
                    val prof = profitText.toDoubleOrNull() ?: 0.0

                    if (itemName.isBlank()) {
                        errorMessage = "Product name cannot be empty."
                        return@Button
                    }
                    if (qty <= 0) {
                        errorMessage = "Quantity must be greater than 0."
                        return@Button
                    }

                    onConfirm(
                        itemName,
                        qty,
                        rev,
                        prof,
                        selectedEmployee,
                        customerName,
                        photoBase64
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = DrinkStockPink),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
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
private fun RewardedVideoAdCard(
    title: String = "Support App & Watch Video Ad",
    subtitle: String = "Watch a video ad to earn bonus features and support DrinkStock development.",
    buttonLabel: String = "Watch Rewarded Video Ad"
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var isLoadingAd by remember { mutableStateOf(false) }
    var adError by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = DrinkStockPink,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = CokeGray
            )

            Button(
                onClick = {
                    if (activity == null) return@Button
                    isLoadingAd = true
                    adError = null

                    AdMobManager.loadRewardedAd(
                        context = context,
                        onAdLoaded = { rewardedAd ->
                            isLoadingAd = false
                            AdMobManager.showRewardedAd(
                                activity = activity,
                                rewardedAd = rewardedAd,
                                onUserRewarded = {
                                    Toast.makeText(context, "Thank you for watching! Reward earned.", Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        onAdFailed = { err ->
                            isLoadingAd = false
                            adError = "Ad unavailable ($err). Please try again later."
                        }
                    )
                },
                enabled = !isLoadingAd,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DrinkStockPink),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoadingAd) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Loading Video Ad...", fontSize = 14.sp)
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(buttonLabel, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            adError?.let { err ->
                Text(text = err, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
            }
        }
    }
}
