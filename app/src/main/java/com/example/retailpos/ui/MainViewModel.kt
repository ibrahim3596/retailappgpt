package com.example.retailpos.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.retailpos.data.local.AppDatabase
import com.example.retailpos.data.local.entity.*
import com.example.retailpos.engine.barcode.BarcodeNormalizer
import com.example.retailpos.repository.CartItem
import com.example.retailpos.repository.CustomerRepository
import com.example.retailpos.repository.InventoryRepository
import com.example.retailpos.repository.PosRepository
import com.example.retailpos.auth.UserPermissions
import com.example.retailpos.auth.userRole
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.Calendar

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val db = AppDatabase.getDatabase(application)
    val posRepo = PosRepository(db)
    val inventoryRepo = InventoryRepository(db)
    val customerRepo = CustomerRepository(db)
    private val sessionManager = com.example.retailpos.util.SessionManager(application)
    private val authService = com.example.retailpos.auth.SupabaseAuthService()

    private val _isSetupComplete = MutableStateFlow<Boolean?>(null)
    val isSetupComplete: StateFlow<Boolean?> = _isSetupComplete.asStateFlow()

    private val _loggedInUserId = MutableStateFlow<String?>(null)
    val loggedInUserId: StateFlow<String?> = _loggedInUserId.asStateFlow()

    val currentStoreId = MutableStateFlow("STORE-DEFAULT-001")

    val supabaseAuthState: StateFlow<com.example.retailpos.auth.AuthState> = authService.authState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.retailpos.auth.AuthState.Unauthenticated)

    private val _pendingSupabaseUser = MutableStateFlow<com.example.retailpos.auth.AuthState.Authenticated?>(null)
    val pendingSupabaseUser = _pendingSupabaseUser.asStateFlow()

    val currentUser: StateFlow<UserEntity?> = loggedInUserId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else db.userDao().getUserFlow(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allUsers: StateFlow<List<UserEntity>> = currentStoreId
        .flatMapLatest { db.userDao().getAllUsers(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStores: StateFlow<List<StoreEntity>> = db.storeDao().getAllStores()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    data class HeldCart(
        val id: String = UUID.randomUUID().toString(),
        val note: String,
        val items: List<CartItem>,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val _sharedCartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val sharedCartItems: StateFlow<List<CartItem>> = _sharedCartItems.asStateFlow()

    private val _heldCarts = MutableStateFlow<List<HeldCart>>(emptyList())
    val heldCarts: StateFlow<List<HeldCart>> = _heldCarts.asStateFlow()

    fun holdCurrentCart(note: String = "Customer Bill") {
        val current = _sharedCartItems.value
        if (current.isNotEmpty()) {
            val held = HeldCart(
                note = if (note.isBlank()) "Cart #${_heldCarts.value.size + 1}" else note,
                items = current
            )
            _heldCarts.value = _heldCarts.value + held
            _sharedCartItems.value = emptyList()
        }
    }

    fun restoreHeldCart(heldCartId: String) {
        val target = _heldCarts.value.find { it.id == heldCartId } ?: return
        _heldCarts.value = _heldCarts.value.filter { it.id != heldCartId }
        _sharedCartItems.value = target.items
    }

    fun deleteHeldCart(heldCartId: String) {
        _heldCarts.value = _heldCarts.value.filter { it.id != heldCartId }
    }

    fun setCartItems(items: List<CartItem>) {
        _sharedCartItems.value = items
    }

    fun addToCartDirectly(product: ProductEntity) {
        val currentList = _sharedCartItems.value.toMutableList()
        val existingIdx = currentList.indexOfFirst { it.product.id == product.id }
        if (existingIdx >= 0) {
            val current = currentList[existingIdx]
            currentList[existingIdx] = current.copy(quantity = current.quantity + 1.0)
        } else {
            currentList.add(CartItem(product = product, quantity = 1.0))
        }
        _sharedCartItems.value = currentList
    }

    fun updateCartQuantity(productId: String, delta: Double) {
        val currentList = _sharedCartItems.value.toMutableList()
        val existingIdx = currentList.indexOfFirst { it.product.id == productId }
        if (existingIdx >= 0) {
            val current = currentList[existingIdx]
            val newQty = current.quantity + delta
            if (newQty <= 0) {
                currentList.removeAt(existingIdx)
            } else {
                currentList[existingIdx] = current.copy(quantity = newQty)
            }
            _sharedCartItems.value = currentList
        }
    }

    fun clearCart() {
        _sharedCartItems.value = emptyList()
    }

    val currentStore: StateFlow<StoreEntity?> = currentStoreId
        .flatMapLatest { db.storeDao().getStoreFlow(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val products: StateFlow<List<ProductEntity>> = currentStoreId
        .flatMapLatest { posRepo.getProducts(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts: StateFlow<List<ProductEntity>> = currentStoreId
        .flatMapLatest { inventoryRepo.getLowStockProducts(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expiringSoonBatches: StateFlow<List<BatchEntity>> = currentStoreId
        .flatMapLatest { inventoryRepo.getExpiringBatches(it, 30) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStockMovements: StateFlow<List<StockMovementEntity>> = currentStoreId
        .flatMapLatest { inventoryRepo.getStockMovements(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<CustomerEntity>> = currentStoreId
        .flatMapLatest { customerRepo.getAllCustomers(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val invoices: StateFlow<List<InvoiceWithItems>> = currentStoreId
        .flatMapLatest { db.invoiceDao().getAllInvoices(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _analyticsRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val analyticsRange: StateFlow<Pair<Long, Long>?> = _analyticsRange.asStateFlow()

    fun setAnalyticsRange(start: Long, end: Long) {
        _analyticsRange.value = start to end
    }

    val filteredInvoices: StateFlow<List<InvoiceWithItems>> = combine(currentStoreId, _analyticsRange) { storeId: String, range: Pair<Long, Long>? ->
        storeId to range
    }.flatMapLatest { (storeId, range) ->
        if (range == null) {
            // Default to today
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis
            val end = System.currentTimeMillis()
            db.invoiceDao().getInvoicesWithItemsForRange(storeId, start, end)
        } else {
            db.invoiceDao().getInvoicesWithItemsForRange(storeId, range.first, range.second)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unresolvedConflicts: StateFlow<List<SyncConflictEntity>> = currentStoreId
        .flatMapLatest { db.syncDao().getUnresolvedConflicts(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            sessionManager.isSetupComplete.collect { _isSetupComplete.value = it }
        }
        viewModelScope.launch {
            sessionManager.loggedInUserId.collect { _loggedInUserId.value = it }
        }
        viewModelScope.launch {
            sessionManager.currentStoreId.collect { id ->
                if (id != null) currentStoreId.value = id
            }
        }
        // React to Supabase Auth State
        viewModelScope.launch {
            supabaseAuthState.collect { state ->
                if (state is com.example.retailpos.auth.AuthState.Authenticated) {
                    val user = db.userDao().getUserBySupabaseId(state.userId)
                    if (user != null) {
                        // Linked! Log in locally.
                        sessionManager.setLoggedInUserId(user.id)
                        sessionManager.setCurrentStoreId(user.storeId)
                        _pendingSupabaseUser.value = null
                    } else {
                        // Not linked yet. Set pending to show linking UI
                        _pendingSupabaseUser.value = state
                    }
                } else if (state is com.example.retailpos.auth.AuthState.Unauthenticated) {
                    _pendingSupabaseUser.value = null
                }
            }
        }
        // Session Invalidation
        viewModelScope.launch {
            currentUser.collect { user ->
                val currentId = _loggedInUserId.value
                if (user == null && currentId != null) {
                    logout()
                }
            }
        }
    }

    fun loginWithGoogle() {
        viewModelScope.launch {
            authService.loginWithGoogle()
        }
    }

    fun linkGoogleAccountToOwner(username: String, pin: String, onResult: (Boolean, String?) -> Unit) {
        val pending = _pendingSupabaseUser.value ?: return onResult(false, "No authenticated Google account")
        viewModelScope.launch {
            val user = db.userDao().getUserByUsername(username.lowercase(), currentStoreId.value)
            if (user != null && user.role == "OWNER" && com.example.retailpos.util.PasswordHasher.verifyPassword(pin, user.pinHash)) {
                if (user.supabaseUserId != null && user.supabaseUserId != pending.userId) {
                    onResult(false, "This owner account is already linked to another Google identity")
                    return@launch
                }
                
                val updated = user.copy(supabaseUserId = pending.userId)
                db.userDao().insertUser(updated)
                
                sessionManager.setLoggedInUserId(user.id)
                sessionManager.setCurrentStoreId(user.storeId)
                _pendingSupabaseUser.value = null
                onResult(true, null)
            } else {
                onResult(false, "Invalid owner credentials or role")
            }
        }
    }

    fun cancelGoogleLinking() {
        viewModelScope.launch {
            authService.logout()
            _pendingSupabaseUser.value = null
        }
    }

    fun setupStoreAndOwnerWithGoogle(storeName: String, ownerName: String, phone: String, pin: String) {
        val pending = _pendingSupabaseUser.value ?: return
        viewModelScope.launch {
            val storeId = "STORE-" + UUID.randomUUID().toString().take(8).uppercase()
            val userId = "USER-" + UUID.randomUUID().toString().take(8).uppercase()

            val store = StoreEntity(
                id = storeId,
                name = storeName,
                ownerName = ownerName,
                phone = phone,
                updatedAt = System.currentTimeMillis()
            )
            db.storeDao().insertOrUpdateStore(store)

            val owner = UserEntity(
                id = userId,
                storeId = storeId,
                username = ownerName.filter { it.isLetterOrDigit() }.lowercase(),
                fullName = ownerName,
                role = "OWNER",
                pinHash = com.example.retailpos.util.PasswordHasher.hashPassword(pin),
                supabaseUserId = pending.userId
            )
            db.userDao().insertUser(owner)

            seedDefaultCatalog(storeId)

            sessionManager.setCurrentStoreId(storeId)
            sessionManager.setLoggedInUserId(userId)
            sessionManager.setSetupComplete(true)
            _pendingSupabaseUser.value = null
        }
    }

    fun setupStoreAndOwner(storeName: String, ownerName: String, phone: String, pin: String) {
        viewModelScope.launch {
            val storeId = "STORE-" + UUID.randomUUID().toString().take(8).uppercase()
            val userId = "USER-" + UUID.randomUUID().toString().take(8).uppercase()

            val store = StoreEntity(
                id = storeId,
                name = storeName,
                ownerName = ownerName,
                phone = phone,
                updatedAt = System.currentTimeMillis()
            )
            db.storeDao().insertOrUpdateStore(store)

            val owner = UserEntity(
                id = userId,
                storeId = storeId,
                username = ownerName.filter { it.isLetterOrDigit() }.lowercase(),
                fullName = ownerName,
                role = "OWNER",
                pinHash = com.example.retailpos.util.PasswordHasher.hashPassword(pin)
            )
            db.userDao().insertUser(owner)

            // Seed some default products for a better first experience
            seedDefaultCatalog(storeId)

            sessionManager.setCurrentStoreId(storeId)
            sessionManager.setLoggedInUserId(userId)
            sessionManager.setSetupComplete(true)
        }
    }

    fun login(username: String, pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val user = db.userDao().getUserByUsername(username, currentStoreId.value)
            if (user != null && com.example.retailpos.util.PasswordHasher.verifyPassword(pin, user.pinHash)) {
                sessionManager.setLoggedInUserId(user.id)
                sessionManager.setCurrentStoreId(user.storeId)
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authService.logout()
            sessionManager.setLoggedInUserId(null)
            _sharedCartItems.value = emptyList()
        }
    }

    fun saveProductWithAuth(product: ProductEntity, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val user = currentUser.value
            if (!UserPermissions.canManageProducts(user.userRole)) {
                onResult(false)
                return@launch
            }
            inventoryRepo.saveProduct(product)
            onResult(true)
        }
    }

    fun addBatchWithAuth(batch: BatchEntity, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val user = currentUser.value
            if (!UserPermissions.canAccessInventory(user.userRole)) {
                onResult(false)
                return@launch
            }
            inventoryRepo.addBatch(batch)
            onResult(true)
        }
    }

    fun quickRestockWithAuth(product: ProductEntity, addQty: Double, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val user = currentUser.value
            if (!UserPermissions.canAdjustStock(user.userRole)) {
                onResult(false)
                return@launch
            }
            inventoryRepo.quickRestock(
                storeId = product.storeId,
                productId = product.id,
                addQty = addQty,
                mrp = product.mrp,
                sellingPrice = product.sellingPrice,
                purchasePrice = product.purchasePrice
            )
            onResult(true)
        }
    }

    fun adjustStockWithAuth(product: ProductEntity, quantityChange: Double, type: com.example.retailpos.data.local.entity.StockMovementType, notes: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val user = currentUser.value
            if (!UserPermissions.canAdjustStock(user.userRole)) {
                onResult(false)
                return@launch
            }
            val success = inventoryRepo.adjustStock(
                storeId = product.storeId,
                productId = product.id,
                quantityChange = quantityChange,
                type = type,
                notes = notes
            )
            onResult(success)
        }
    }

    private suspend fun seedDefaultCatalog(storeId: String) {
        // Implementation from old seedDefaultStoreAndCatalogIfNeeded but for a specific storeId
        val p1 = ProductEntity(
            id = UUID.randomUUID().toString(),
            storeId = storeId,
            sku = "SKU-001",
            barcode = "8901030300018",
            normalizedBarcode = BarcodeNormalizer.normalize("8901030300018").canonicalGtin,
            name = "Amul Taaza Toned Milk",
            brand = "Amul",
            category = "Dairy",
            variant = "500 ml",
            packSize = "500ml Pouch",
            hsnCode = "0401",
            unit = "PCS",
            mrp = 28.0,
            sellingPrice = 27.0,
            purchasePrice = 24.50,
            gstRate = 5.0,
            taxType = TaxType.INCLUSIVE,
            currentStock = 45.0,
            minStock = 10.0,
            verificationStatus = VerificationStatus.VERIFIED
        )

            val p2 = ProductEntity(
                id = UUID.randomUUID().toString(),
                storeId = storeId,
                sku = "SKU-002",
                barcode = "8901058852025",
                normalizedBarcode = BarcodeNormalizer.normalize("8901058852025").canonicalGtin,
                name = "Aashirvaad Shudh Chakki Atta",
                brand = "Aashirvaad",
                category = "Staples",
                variant = "5 kg",
                packSize = "5kg Bag",
                hsnCode = "1101",
                unit = "PACK",
                mrp = 265.0,
                sellingPrice = 250.0,
                purchasePrice = 225.0,
                gstRate = 5.0,
                taxType = TaxType.INCLUSIVE,
                currentStock = 20.0,
                minStock = 5.0,
                verificationStatus = VerificationStatus.VERIFIED
            )

            val p3 = ProductEntity(
                id = UUID.randomUUID().toString(),
                storeId = storeId,
                sku = "SKU-003",
                barcode = "8901030010122",
                normalizedBarcode = BarcodeNormalizer.normalize("8901030010122").canonicalGtin,
                name = "Tata Salt Vacuum Evaporated",
                brand = "Tata",
                category = "Staples",
                variant = "1 kg",
                packSize = "1kg Pack",
                hsnCode = "2501",
                unit = "PCS",
                mrp = 28.0,
                sellingPrice = 28.0,
                purchasePrice = 23.0,
                gstRate = 0.0,
                taxType = TaxType.INCLUSIVE,
                currentStock = 80.0,
                minStock = 15.0,
                verificationStatus = VerificationStatus.VERIFIED
            )

            val p4 = ProductEntity(
                id = UUID.randomUUID().toString(),
                storeId = storeId,
                sku = "SKU-004",
                barcode = "8901207000102",
                normalizedBarcode = BarcodeNormalizer.normalize("8901207000102").canonicalGtin,
                name = "Parle-G Gold Biscuit",
                brand = "Parle",
                category = "Snacks",
                variant = "100 g",
                packSize = "100g Pack",
                hsnCode = "1905",
                unit = "PCS",
                mrp = 10.0,
                sellingPrice = 10.0,
                purchasePrice = 8.50,
                gstRate = 18.0,
                taxType = TaxType.INCLUSIVE,
                currentStock = 120.0,
                minStock = 20.0,
                verificationStatus = VerificationStatus.VERIFIED
            )

            val p5 = ProductEntity(
                id = UUID.randomUUID().toString(),
                storeId = storeId,
                sku = "SKU-005",
                barcode = "8901030000001",
                normalizedBarcode = BarcodeNormalizer.normalize("8901030000001").canonicalGtin,
                name = "Surf Excel Easy Wash Powder",
                brand = "Surf Excel",
                category = "Household",
                variant = "1 kg",
                packSize = "1kg Box",
                hsnCode = "3402",
                unit = "PCS",
                mrp = 150.0,
                sellingPrice = 142.0,
                purchasePrice = 122.0,
                gstRate = 18.0,
                taxType = TaxType.INCLUSIVE,
                currentStock = 3.0,
                minStock = 10.0,
                verificationStatus = VerificationStatus.VERIFIED
            )

            val defaultProducts = listOf(p1, p2, p3, p4, p5)

            for (prod in defaultProducts) {
                db.productDao().insertProduct(prod)

                val batch = BatchEntity(
                    id = UUID.randomUUID().toString(),
                    productId = prod.id,
                    storeId = storeId,
                    batchNumber = "BATCH-" + System.currentTimeMillis().toString().takeLast(6),
                    mfd = "01/2026",
                    expiryDate = System.currentTimeMillis() + (120L * 24 * 3600 * 1000),
                    mrp = prod.mrp,
                    sellingPrice = prod.sellingPrice,
                    purchasePrice = prod.purchasePrice,
                    initialQty = prod.currentStock,
                    remainingQty = prod.currentStock
                )
                db.batchDao().insertBatch(batch)
            }

            val customer = CustomerEntity(
                id = UUID.randomUUID().toString(),
                storeId = storeId,
                name = "Ramesh Kumar (Local Resident)",
                phone = "9820098200",
                currentBalance = 450.0,
                creditLimit = 5000.0
            )
            customerRepo.saveCustomer(customer)
    }

    fun updateStoreDetails(name: String, gstin: String, address: String, phone: String) {
        viewModelScope.launch {
            val user = currentUser.value
            if (!UserPermissions.canUpdateStoreProfile(user.userRole)) return@launch

            val store = currentStore.value ?: return@launch
            val updated = store.copy(
                name = name,
                gstin = gstin,
                address = address,
                phone = phone,
                updatedAt = System.currentTimeMillis()
            )
            db.storeDao().insertOrUpdateStore(updated)
        }
    }

    fun addStaffMember(username: String, fullName: String, pin: String, role: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val currentUserVal = currentUser.value
            if (!UserPermissions.canManageStaff(currentUserVal.userRole)) {
                onResult(false)
                return@launch
            }

            val existing = db.userDao().getUserByUsername(username, currentStoreId.value)
            if (existing != null) {
                onResult(false)
                return@launch
            }
            val newUser = UserEntity(
                id = "USER-" + UUID.randomUUID().toString().take(8).uppercase(),
                storeId = currentStoreId.value,
                username = username.lowercase().filter { it.isLetterOrDigit() },
                fullName = fullName,
                role = role,
                pinHash = com.example.retailpos.util.PasswordHasher.hashPassword(pin)
            )
            db.userDao().insertUser(newUser)
            onResult(true)
        }
    }

    fun removeStaffMember(user: UserEntity) {
        viewModelScope.launch {
            val currentUserVal = currentUser.value
            if (!UserPermissions.canManageStaff(currentUserVal.userRole)) return@launch

            // Cannot remove the owner through this UI easily to prevent lockout
            if (user.role == "OWNER") return@launch
            db.userDao().deleteUser(user)
        }
    }

    fun switchStore(newStoreId: String) {
        currentStoreId.value = newStoreId
    }

    fun createNewStore(name: String, gstin: String, address: String, phone: String, ownerName: String, ownerUsername: String, ownerPin: String) {
        viewModelScope.launch {
            val newStoreId = "STORE-" + UUID.randomUUID().toString().take(8).uppercase()
            val newStore = StoreEntity(
                id = newStoreId,
                name = name,
                ownerName = ownerName,
                gstin = gstin,
                address = address,
                phone = phone,
                updatedAt = System.currentTimeMillis()
            )
            db.storeDao().insertOrUpdateStore(newStore)

            val owner = UserEntity(
                id = "USER-" + UUID.randomUUID().toString().take(8).uppercase(),
                storeId = newStoreId,
                username = ownerUsername.lowercase(),
                fullName = ownerName,
                role = "OWNER",
                pinHash = com.example.retailpos.util.PasswordHasher.hashPassword(ownerPin)
            )
            db.userDao().insertUser(owner)
            
            // Auto switch to new store
            currentStoreId.value = newStoreId
        }
    }
}
