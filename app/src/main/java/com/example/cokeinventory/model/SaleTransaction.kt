package com.example.cokeinventory.model

data class SaleTransaction(
    val id: String = "",
    val itemId: String = "",
    val itemName: String = "",
    val quantitySold: Int = 0,
    val totalRevenue: Double = 0.0,
    val profitMade: Double = 0.0,
    val employeeName: String = "Ngelo",
    val customerName: String = "",
    val photoBase64: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
