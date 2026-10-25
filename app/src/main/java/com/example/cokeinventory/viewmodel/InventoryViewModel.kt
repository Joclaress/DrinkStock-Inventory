package com.example.cokeinventory.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.cokeinventory.model.CokeItem
import com.example.cokeinventory.model.SaleTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

val DEFAULT_EMPLOYEES = listOf("Ngelo", "Epoy", "Paul", "Jonard", "Ricky", "Owner")

data class InventoryUiState(
    val currentUser: FirebaseUser? = null,
    val isGuestMode: Boolean = false,
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val authError: String? = null,
    val items: List<CokeItem> = emptyList(),
    val salesTransactions: List<SaleTransaction> = emptyList(),
    val isFirestoreActive: Boolean = false,
    val message: String? = null
) {
    val totalStockCount: Int get() = items.sumOf { it.stockQuantity }

    val totalProfit: Double
        get() {
            val profitFromSales = salesTransactions.sumOf { it.profitMade }
            val profitFromItems = items.sumOf { it.totalProfit }
            return maxOf(profitFromSales, profitFromItems)
        }

    val totalRevenue: Double
        get() {
            val revenueFromSales = salesTransactions.sumOf { it.totalRevenue }
            val revenueFromItems = items.sumOf { it.totalSalesRevenue }
            return maxOf(revenueFromSales, revenueFromItems)
        }

    val totalStockValue: Double get() = items.sumOf { it.stockValue }

    val totalUnitsSold: Int
        get() {
            val unitsFromSales = salesTransactions.sumOf { it.quantitySold }
            val unitsFromItems = items.sumOf { it.unitsSold }
            return maxOf(unitsFromSales, unitsFromItems)
        }
}

class InventoryViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    private var auth: FirebaseAuth? = null
    private var db: FirebaseFirestore? = null
    private var firestoreListener: ListenerRegistration? = null
    private var softdrinksListener: ListenerRegistration? = null
    private var salesListener: ListenerRegistration? = null
    private var hasUserClearedDefaultSeed = false

    init {
        try {
            auth = FirebaseAuth.getInstance()
            db = FirebaseFirestore.getInstance()
            checkCurrentUser()
        } catch (e: Exception) {
            Log.e("InventoryViewModel", "Firebase init warning: ${e.localizedMessage}")
            loadDemoItems()
        }
    }

    private fun getUserInventoryRef(): CollectionReference? {
        val uid = auth?.currentUser?.uid ?: return null
        return db?.collection("users")?.document(uid)?.collection("coke_inventory")
    }

    private fun getUserSalesRef(): CollectionReference? {
        val uid = auth?.currentUser?.uid ?: return null
        return db?.collection("users")?.document(uid)?.collection("sales_transactions")
    }

    private fun checkCurrentUser() {
        val user = auth?.currentUser
        if (user != null) {
            _uiState.update {
                it.copy(
                    currentUser = user,
                    isLoggedIn = true,
                    isGuestMode = user.isAnonymous
                )
            }
            attachFirestoreListener()
            attachSalesListener()
            removeAllDefaultSampleItems()
        } else {
            loadDemoItems()
        }
    }

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.update { it.copy(authError = "Email and Password cannot be empty.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, authError = null) }

        val authRef = auth
        if (authRef == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isLoggedIn = true,
                    isGuestMode = true,
                    authError = null
                )
            }
            return
        }

        authRef.signInWithEmailAndPassword(email.trim(), pass)
            .addOnSuccessListener { result ->
                _uiState.update {
                    it.copy(
                        currentUser = result.user,
                        isLoggedIn = true,
                        isGuestMode = false,
                        isLoading = false,
                        authError = null
                    )
                }
                attachFirestoreListener()
                attachSalesListener()
            }
            .addOnFailureListener { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        authError = e.localizedMessage ?: "Login failed. Please check your credentials."
                    )
                }
            }
    }

    fun signUp(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.update { it.copy(authError = "Email and Password cannot be empty.") }
            return
        }
        if (pass.length < 6) {
            _uiState.update { it.copy(authError = "Password must be at least 6 characters.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, authError = null) }

        val authRef = auth
        if (authRef == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isLoggedIn = true,
                    isGuestMode = true
                )
            }
            return
        }

        authRef.createUserWithEmailAndPassword(email.trim(), pass)
            .addOnSuccessListener { result ->
                _uiState.update {
                    it.copy(
                        currentUser = result.user,
                        isLoggedIn = true,
                        isGuestMode = false,
                        isLoading = false,
                        authError = null
                    )
                }
                seedInitialDataIfEmpty()
                attachFirestoreListener()
                attachSalesListener()
            }
            .addOnFailureListener { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        authError = e.localizedMessage ?: "Sign up failed."
                    )
                }
            }
    }

    fun loginAsGuest() {
        _uiState.update { it.copy(isLoading = true, authError = null) }

        val authRef = auth
        if (authRef != null) {
            authRef.signInAnonymously()
                .addOnSuccessListener { result ->
                    _uiState.update {
                        it.copy(
                            currentUser = result.user,
                            isLoggedIn = true,
                            isGuestMode = true,
                            isLoading = false
                        )
                    }
                    attachFirestoreListener()
                    attachSalesListener()
                }
                .addOnFailureListener {
                    _uiState.update {
                        it.copy(
                            isLoggedIn = true,
                            isGuestMode = true,
                            isLoading = false
                        )
                    }
                    if (_uiState.value.items.isEmpty()) loadDemoItems()
                }
        } else {
            _uiState.update {
                it.copy(
                    isLoggedIn = true,
                    isGuestMode = true,
                    isLoading = false
                )
            }
            if (_uiState.value.items.isEmpty()) loadDemoItems()
        }
    }

    fun signOut() {
        firestoreListener?.remove()
        softdrinksListener?.remove()
        salesListener?.remove()
        firestoreListener = null
        softdrinksListener = null
        salesListener = null
        salesListener = null
        try {
            auth?.signOut()
        } catch (_: Exception) {}
        _uiState.update {
            InventoryUiState(
                isLoggedIn = false,
                isGuestMode = false,
                items = emptyList(),
                salesTransactions = emptyList()
            )
        }
    }

    fun clearAuthError() {
        _uiState.update { it.copy(authError = null) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun attachFirestoreListener() {
        val uid = auth?.currentUser?.uid ?: run {
            if (_uiState.value.items.isEmpty()) loadDemoItems()
            return
        }
        val dbRef = db ?: run {
            if (_uiState.value.items.isEmpty()) loadDemoItems()
            return
        }

        val cokeCollection = dbRef.collection("users").document(uid).collection("coke_inventory")
        val softdrinksCollection = dbRef.collection("users").document(uid).collection("softdrinks_inventory")

        firestoreListener?.remove()
        softdrinksListener?.remove()

        var cokeList = emptyList<CokeItem>()
        var softdrinksList = emptyList<CokeItem>()

        val defaultSampleNames = setOf(
            "Coca-Cola Classic (330ml Can)",
            "Sprite Lemon-Lime (500ml Bottle)",
            "Royal Tru Orange (1.5L Bottle)",
            "Pepsi Cola (330ml Can)",
            "Mountain Dew (330ml Can)"
        )

        fun updateCombinedItems() {
            val combined = (cokeList + softdrinksList)
                .filterNot { it.name in defaultSampleNames }
                .distinctBy { it.id.ifEmpty { it.name } }

            _uiState.update {
                it.copy(
                    items = combined,
                    isFirestoreActive = true
                )
            }
        }

        firestoreListener = cokeCollection.addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                cokeList = snapshot.documents.mapNotNull { doc ->
                    try {
                        CokeItem(
                            id = doc.id,
                            name = doc.getString("name") ?: "Softdrink",
                            category = doc.getString("category") ?: "Can",
                            stockQuantity = (doc.getLong("stockQuantity") ?: 0L).toInt(),
                            costPrice = doc.getDouble("costPrice") ?: 0.0,
                            sellingPrice = doc.getDouble("sellingPrice") ?: 0.0,
                            unitsSold = (doc.getLong("unitsSold") ?: 0L).toInt(),
                            photoBase64 = doc.getString("photoBase64"),
                            addedByEmployee = doc.getString("addedByEmployee") ?: "Ngelo"
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                updateCombinedItems()
            }
        }

        softdrinksListener = softdrinksCollection.addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                softdrinksList = snapshot.documents.mapNotNull { doc ->
                    try {
                        CokeItem(
                            id = doc.id,
                            name = doc.getString("name") ?: "Softdrink",
                            category = doc.getString("category") ?: "Can",
                            stockQuantity = (doc.getLong("stockQuantity") ?: 0L).toInt(),
                            costPrice = doc.getDouble("costPrice") ?: 0.0,
                            sellingPrice = doc.getDouble("sellingPrice") ?: 0.0,
                            unitsSold = (doc.getLong("unitsSold") ?: 0L).toInt(),
                            photoBase64 = doc.getString("photoBase64"),
                            addedByEmployee = doc.getString("addedByEmployee") ?: "Ngelo"
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                updateCombinedItems()
            }
        }
    }

    private fun attachSalesListener() {
        val collectionRef = getUserSalesRef() ?: return
        salesListener?.remove()

        salesListener = collectionRef
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val sales = snapshot.documents.mapNotNull { doc ->
                        try {
                            SaleTransaction(
                                id = doc.id,
                                itemId = doc.getString("itemId") ?: "",
                                itemName = doc.getString("itemName") ?: "",
                                quantitySold = (doc.getLong("quantitySold") ?: 0L).toInt(),
                                totalRevenue = doc.getDouble("totalRevenue") ?: 0.0,
                                profitMade = doc.getDouble("profitMade") ?: 0.0,
                                employeeName = doc.getString("employeeName") ?: "Ngelo",
                                customerName = doc.getString("customerName") ?: "",
                                photoBase64 = doc.getString("photoBase64"),
                                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }.sortedByDescending { it.timestamp }

                    _uiState.update { it.copy(salesTransactions = sales) }
                }
            }
    }

    private fun seedInitialDataIfEmpty() {
        // Default sample items removed
    }

    private fun loadDemoItems() {
        _uiState.update { it.copy(items = emptyList()) }
    }

    fun addOrUpdateCokeItem(
        id: String? = null,
        name: String,
        category: String,
        stockQuantity: Int,
        costPrice: Double,
        sellingPrice: Double,
        addedByEmployee: String = "Ngelo",
        photoBase64: String? = null,
        unitsSold: Int = 0
    ) {
        if (name.isBlank()) {
            _uiState.update { it.copy(message = "Item name cannot be empty.") }
            return
        }

        val collection = getUserInventoryRef()
        if (collection != null && _uiState.value.isFirestoreActive) {
            val data = hashMapOf<String, Any?>(
                "name" to name,
                "category" to category,
                "stockQuantity" to stockQuantity,
                "costPrice" to costPrice,
                "sellingPrice" to sellingPrice,
                "addedByEmployee" to addedByEmployee,
                "unitsSold" to unitsSold
            )
            if (photoBase64 != null) {
                data["photoBase64"] = photoBase64
            }

            if (id.isNullOrBlank()) {
                collection.add(data as Map<String, Any>)
                    .addOnSuccessListener {
                        _uiState.update { it.copy(message = "Added '$name'!") }
                    }
            } else {
                collection.document(id).update(data as Map<String, Any>)
                    .addOnSuccessListener {
                        _uiState.update { it.copy(message = "Updated '$name' successfully!") }
                    }
            }
        } else {
            // Local state update
            _uiState.update { state ->
                val currentList = state.items.toMutableList()
                if (id.isNullOrBlank()) {
                    val newItem = CokeItem(
                        id = System.currentTimeMillis().toString(),
                        name = name,
                        category = category,
                        stockQuantity = stockQuantity,
                        costPrice = costPrice,
                        sellingPrice = sellingPrice,
                        unitsSold = unitsSold,
                        photoBase64 = photoBase64,
                        addedByEmployee = addedByEmployee
                    )
                    currentList.add(newItem)
                } else {
                    val index = currentList.indexOfFirst { it.id == id }
                    if (index != -1) {
                        val existing = currentList[index]
                        currentList[index] = existing.copy(
                            name = name,
                            category = category,
                            stockQuantity = stockQuantity,
                            costPrice = costPrice,
                            sellingPrice = sellingPrice,
                            unitsSold = unitsSold,
                            photoBase64 = photoBase64 ?: existing.photoBase64,
                            addedByEmployee = addedByEmployee
                        )
                    }
                }
                state.copy(
                    items = currentList,
                    message = if (id.isNullOrBlank()) "Added '$name'!" else "Updated '$name'!"
                )
            }
        }
    }

    fun recordSale(
        item: CokeItem,
        quantitySold: Int,
        employeeName: String = "Ngelo",
        customerName: String = "",
        photoBase64: String? = null
    ) {
        if (quantitySold <= 0) return
        if (quantitySold > item.stockQuantity) {
            _uiState.update { it.copy(message = "Not enough stock available!") }
            return
        }

        val newStock = item.stockQuantity - quantitySold
        val newSold = item.unitsSold + quantitySold
        val profitMade = quantitySold * item.profitPerUnit
        val revenueMade = quantitySold * item.sellingPrice
        val cleanedNote = customerName.trim()

        val transactionData = hashMapOf<String, Any?>(
            "itemId" to item.id,
            "itemName" to item.name,
            "quantitySold" to quantitySold,
            "totalRevenue" to revenueMade,
            "profitMade" to profitMade,
            "employeeName" to employeeName,
            "customerName" to cleanedNote,
            "photoBase64" to photoBase64,
            "timestamp" to System.currentTimeMillis()
        )

        val noteInfo = if (cleanedNote.isNotBlank()) " ($cleanedNote)" else ""

        val inventoryRef = getUserInventoryRef()
        val salesRef = getUserSalesRef()
        if (inventoryRef != null && salesRef != null && _uiState.value.isFirestoreActive && item.id.isNotBlank()) {
            inventoryRef.document(item.id).update(
                mapOf(
                    "stockQuantity" to newStock,
                    "unitsSold" to newSold
                )
            ).addOnSuccessListener {
                salesRef.add(transactionData as Map<String, Any>)
                _uiState.update {
                    it.copy(message = "Recorded sale of $quantitySold unit(s)$noteInfo! Profit: ₱${String.format("%.2f", profitMade)}")
                }
            }
        } else {
            _uiState.update { state ->
                val updatedList = state.items.map {
                    if (it.id == item.id) {
                        it.copy(stockQuantity = newStock, unitsSold = newSold)
                    } else it
                }
                val localTx = SaleTransaction(
                    id = System.currentTimeMillis().toString(),
                    itemId = item.id,
                    itemName = item.name,
                    quantitySold = quantitySold,
                    totalRevenue = revenueMade,
                    profitMade = profitMade,
                    employeeName = employeeName,
                    customerName = cleanedNote,
                    photoBase64 = photoBase64
                )
                state.copy(
                    items = updatedList,
                    salesTransactions = listOf(localTx) + state.salesTransactions,
                    message = "Recorded sale of $quantitySold unit(s)$noteInfo! Profit: ₱${String.format("%.2f", profitMade)}"
                )
            }
        }
    }

    fun updateSaleTransaction(
        transactionId: String,
        newItemName: String,
        newQuantitySold: Int,
        newTotalRevenue: Double,
        newProfitMade: Double,
        newEmployeeName: String,
        newCustomerName: String,
        newPhotoBase64: String?
    ) {
        val salesRef = getUserSalesRef()

        val updates = mapOf<String, Any?>(
            "itemName" to newItemName,
            "quantitySold" to newQuantitySold,
            "totalRevenue" to newTotalRevenue,
            "profitMade" to newProfitMade,
            "employeeName" to newEmployeeName,
            "customerName" to newCustomerName,
            "photoBase64" to newPhotoBase64
        )

        if (salesRef != null && _uiState.value.isFirestoreActive && transactionId.isNotBlank()) {
            salesRef.document(transactionId).update(updates as Map<String, Any>)
                .addOnSuccessListener {
                    _uiState.update { it.copy(message = "Updated sale details!") }
                }
        } else {
            _uiState.update { state ->
                val updatedSales = state.salesTransactions.map { tx ->
                    if (tx.id == transactionId) {
                        tx.copy(
                            itemName = newItemName,
                            quantitySold = newQuantitySold,
                            totalRevenue = newTotalRevenue,
                            profitMade = newProfitMade,
                            employeeName = newEmployeeName,
                            customerName = newCustomerName,
                            photoBase64 = newPhotoBase64 ?: tx.photoBase64
                        )
                    } else tx
                }
                state.copy(
                    salesTransactions = updatedSales,
                    message = "Updated sale details!"
                )
            }
        }
    }

    fun addStock(item: CokeItem, quantityToAdd: Int) {
        if (quantityToAdd <= 0) return
        val newStock = item.stockQuantity + quantityToAdd

        val collection = getUserInventoryRef()
        if (collection != null && _uiState.value.isFirestoreActive && item.id.isNotBlank()) {
            collection.document(item.id).update(
                "stockQuantity", newStock
            ).addOnSuccessListener {
                _uiState.update { it.copy(message = "Added $quantityToAdd stock(s) to ${item.name}") }
            }
        } else {
            _uiState.update { state ->
                val updatedList = state.items.map {
                    if (it.id == item.id) {
                        it.copy(stockQuantity = newStock)
                    } else it
                }
                state.copy(items = updatedList, message = "Added $quantityToAdd stock(s) to ${item.name}")
            }
        }
    }

    fun deleteCokeItem(itemId: String) {
        hasUserClearedDefaultSeed = true

        val uid = auth?.currentUser?.uid
        if (uid != null && db != null) {
            val cokeRef = db?.collection("users")?.document(uid)?.collection("coke_inventory")
            val softdrinksRef = db?.collection("users")?.document(uid)?.collection("softdrinks_inventory")

            cokeRef?.document(itemId)?.delete()
            softdrinksRef?.document(itemId)?.delete()
        }

        _uiState.update { state ->
            val filtered = state.items.filterNot { it.id == itemId || it.name == itemId }
            state.copy(
                items = filtered,
                message = "Item deleted."
            )
        }
    }

    fun removeAllDefaultSampleItems() {
        val defaultSampleNames = setOf(
            "Coca-Cola Classic (330ml Can)",
            "Sprite Lemon-Lime (500ml Bottle)",
            "Royal Tru Orange (1.5L Bottle)",
            "Pepsi Cola (330ml Can)",
            "Mountain Dew (330ml Can)"
        )

        val uid = auth?.currentUser?.uid
        if (uid != null && db != null) {
            val cokeRef = db?.collection("users")?.document(uid)?.collection("coke_inventory")
            val softdrinksRef = db?.collection("users")?.document(uid)?.collection("softdrinks_inventory")

            cokeRef?.get()?.addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    if (doc.getString("name") in defaultSampleNames) {
                        doc.reference.delete()
                    }
                }
            }
            softdrinksRef?.get()?.addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    if (doc.getString("name") in defaultSampleNames) {
                        doc.reference.delete()
                    }
                }
            }
        }

        _uiState.update { state ->
            val cleaned = state.items.filterNot { it.name in defaultSampleNames }
            state.copy(items = cleaned)
        }
    }

    fun resetAllSalesAndProfits() {
        val inventoryRef = getUserInventoryRef()
        val salesRef = getUserSalesRef()

        if (inventoryRef != null && salesRef != null && _uiState.value.isFirestoreActive) {
            inventoryRef.get().addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    doc.reference.update("unitsSold", 0)
                }
            }
            salesRef.get().addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    doc.reference.delete()
                }
            }
            _uiState.update { state ->
                val resetList = state.items.map { it.copy(unitsSold = 0) }
                state.copy(
                    items = resetList,
                    salesTransactions = emptyList(),
                    message = "Sales and profits have been reset to ₱0.00!"
                )
            }
        } else {
            _uiState.update { state ->
                val resetList = state.items.map { it.copy(unitsSold = 0) }
                state.copy(
                    items = resetList,
                    salesTransactions = emptyList(),
                    message = "Sales and profits have been reset to ₱0.00!"
                )
            }
        }
    }
}
