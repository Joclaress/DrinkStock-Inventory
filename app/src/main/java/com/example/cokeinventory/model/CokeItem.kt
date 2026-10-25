package com.example.cokeinventory.model

data class CokeItem(
    val id: String = "",
    val name: String = "",
    val category: String = "Can", // Can, Bottle (500ml), Bottle (1.5L), Crate, etc.
    val stockQuantity: Int = 0,
    val costPrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val unitsSold: Int = 0,
    val photoBase64: String? = null,
    val addedByEmployee: String = "Ngelo"
) {
    // Computed fields for profit & revenue calculations
    val profitPerUnit: Double
        get() = (sellingPrice - costPrice).coerceAtLeast(0.0)

    val totalProfit: Double
        get() = unitsSold * profitPerUnit

    val stockValue: Double
        get() = stockQuantity * costPrice

    val totalSalesRevenue: Double
        get() = unitsSold * sellingPrice
}
