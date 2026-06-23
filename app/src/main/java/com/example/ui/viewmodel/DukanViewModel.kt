package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.Customer
import com.example.data.database.Product
import com.example.data.database.Sale
import com.example.data.repository.DukanRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class ProfitDataPoint(
    val label: String,
    val profit: Double,
    val sales: Double
)

data class DailySalesDataPoint(
    val label: String,
    val revenue: Double,
    val timestamp: Long
)

class DukanViewModel(private val repository: DukanRepository) : ViewModel() {

    val products: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<Customer>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sales: StateFlow<List<Sale>> = repository.allSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val saleItems: StateFlow<List<com.example.data.database.SaleItem>> = repository.allSaleItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calculated reports
    val todayReport: StateFlow<Triple<Double, Double, Double>> = repository.allSales
        .map { salesList ->
            val calendar = Calendar.getInstance()
            val todayDay = calendar.get(Calendar.DAY_OF_YEAR)
            val todayYear = calendar.get(Calendar.YEAR)

            var salesSum = 0.0
            var costSum = 0.0

            for (sale in salesList) {
                val saleCal = Calendar.getInstance().apply { timeInMillis = sale.timestamp }
                if (saleCal.get(Calendar.DAY_OF_YEAR) == todayDay && saleCal.get(Calendar.YEAR) == todayYear) {
                    salesSum += sale.totalPrice
                    costSum += sale.totalCost
                }
            }
            Triple(salesSum, costSum, salesSum - costSum) // Sales, Cost, Profit
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Triple(0.0, 0.0, 0.0))

    // Last 7 days profit/loss data for the graph
    val profitChartData: StateFlow<List<ProfitDataPoint>> = repository.allSales
        .map { salesList ->
            val calendar = Calendar.getInstance()
            val format = SimpleDateFormat("dd MMM", Locale.getDefault())
            
            // Initialize last 7 days map
            val pointsMap = LinkedHashMap<String, Pair<Double, Double>>() // DateStr -> Pair(Sales, Cost)
            for (i in 6 downTo 0) {
                val tempCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                val dateStr = format.format(tempCal.time)
                pointsMap[dateStr] = Pair(0.0, 0.0)
            }

            for (sale in salesList) {
                val saleDateStr = format.format(Date(sale.timestamp))
                if (pointsMap.containsKey(saleDateStr)) {
                    val current = pointsMap[saleDateStr] ?: Pair(0.0, 0.0)
                    pointsMap[saleDateStr] = Pair(current.first + sale.totalPrice, current.second + sale.totalCost)
                }
            }

            pointsMap.map { (date, pair) ->
                ProfitDataPoint(
                    label = date,
                    profit = pair.first - pair.second,
                    sales = pair.first
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Last 30 days daily sales revenue data for the chart
    val thirtyDaysSalesChartData: StateFlow<List<DailySalesDataPoint>> = repository.allSales
        .map { salesList ->
            val calendar = Calendar.getInstance()
            val format = SimpleDateFormat("dd MMM", Locale.getDefault())
            
            // Initialize last 30 days map
            val pointsMap = LinkedHashMap<String, Double>() // DateStr -> Revenue
            val timestampMap = LinkedHashMap<String, Long>() // DateStr -> Timestamp
            for (i in 29 downTo 0) {
                val tempCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                val dateStr = format.format(tempCal.time)
                pointsMap[dateStr] = 0.0
                timestampMap[dateStr] = tempCal.timeInMillis
            }

            for (sale in salesList) {
                val saleDateStr = format.format(Date(sale.timestamp))
                if (pointsMap.containsKey(saleDateStr)) {
                    val currentVal = pointsMap[saleDateStr] ?: 0.0
                    pointsMap[saleDateStr] = currentVal + sale.totalPrice
                }
            }

            pointsMap.map { (date, revenue) ->
                DailySalesDataPoint(
                    label = date,
                    revenue = revenue,
                    timestamp = timestampMap[date] ?: 0L
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Product actions
    fun addProduct(name: String, costPrice: Double, sellingPrice: Double, stock: Int, category: String, minStockThreshold: Int = 5, rating: Int = 5) {
        viewModelScope.launch {
            repository.insertProduct(
                Product(
                    name = name,
                    costPrice = costPrice,
                    sellingPrice = sellingPrice,
                    stock = stock,
                    category = category,
                    minStockThreshold = minStockThreshold,
                    rating = rating
                )
            )
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            repository.updateProduct(product)
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    // Customer actions
    fun addCustomer(name: String, phone: String, initialUdhaar: Double, rating: Int = 5) {
        viewModelScope.launch {
            repository.insertCustomer(
                Customer(
                    name = name,
                    phone = phone,
                    balance = initialUdhaar, // If they already owe us money, set it
                    rating = rating
                )
            )
        }
    }

    fun updateCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.updateCustomer(customer)
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
        }
    }

    fun recordPayment(customer: Customer, amountPaid: Double) {
        viewModelScope.launch {
            // Customer pays back some debt. Subtracting from current Udhaar balance.
            repository.adjustCustomerBalance(customer.id, -amountPaid)
        }
    }

    // Sales action
    fun makeSale(customerId: Int?, customerName: String?, isCreditSale: Boolean, items: List<Pair<Product, Int>>) {
        viewModelScope.launch {
            repository.executeSale(
                customerId = customerId,
                customerName = customerName,
                isCreditSale = isCreditSale,
                items = items
            )
        }
    }

    // Gmail Google authentication state
    private val _isLoggedIn = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _loggedInUserEmail = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val loggedInUserEmail: StateFlow<String?> = _loggedInUserEmail

    private val _loggedInUserName = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val loggedInUserName: StateFlow<String?> = _loggedInUserName

    private val _loggedInUserPhotoUrl = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val loggedInUserPhotoUrl: StateFlow<String?> = _loggedInUserPhotoUrl

    fun login(email: String, name: String, photoUrl: String? = null) {
        _isLoggedIn.value = true
        _loggedInUserEmail.value = email
        _loggedInUserName.value = name
        _loggedInUserPhotoUrl.value = photoUrl
    }

    fun logout() {
        _isLoggedIn.value = false
        _loggedInUserEmail.value = null
        _loggedInUserName.value = null
        _loggedInUserPhotoUrl.value = null
    }

    // Direct JSON Backup & Restore for Local Storage
    fun exportBackupJson(onComplete: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val jsonObject = org.json.JSONObject()
                jsonObject.put("backup_version", 2)
                jsonObject.put("app_owner", "SHAWAL")
                jsonObject.put("created_at", System.currentTimeMillis())
                
                // Export Products
                val productArray = org.json.JSONArray()
                products.value.forEach { product ->
                    val pJson = org.json.JSONObject()
                    pJson.put("id", product.id)
                    pJson.put("name", product.name)
                    pJson.put("costPrice", product.costPrice)
                    pJson.put("sellingPrice", product.sellingPrice)
                    pJson.put("stock", product.stock)
                    pJson.put("category", product.category)
                    pJson.put("minStockThreshold", product.minStockThreshold)
                    pJson.put("rating", product.rating)
                    productArray.put(pJson)
                }
                jsonObject.put("products", productArray)

                // Export Customers
                val customerArray = org.json.JSONArray()
                customers.value.forEach { customer ->
                    val cJson = org.json.JSONObject()
                    cJson.put("id", customer.id)
                    cJson.put("name", customer.name)
                    cJson.put("phone", customer.phone)
                    cJson.put("balance", customer.balance)
                    cJson.put("rating", customer.rating)
                    customerArray.put(cJson)
                }
                jsonObject.put("customers", customerArray)

                // Export Sales
                val saleArray = org.json.JSONArray()
                sales.value.forEach { sale ->
                    val sJson = org.json.JSONObject()
                    sJson.put("id", sale.id)
                    sJson.put("customerId", sale.customerId ?: org.json.JSONObject.NULL)
                    sJson.put("customerName", sale.customerName ?: org.json.JSONObject.NULL)
                    sJson.put("timestamp", sale.timestamp)
                    sJson.put("totalPrice", sale.totalPrice)
                    sJson.put("totalCost", sale.totalCost)
                    saleArray.put(sJson)
                }
                jsonObject.put("sales", saleArray)

                // Export SaleItems
                val saleItemArray = org.json.JSONArray()
                saleItems.value.forEach { saleItem ->
                    val siJson = org.json.JSONObject()
                    siJson.put("id", saleItem.id)
                    siJson.put("saleId", saleItem.saleId)
                    siJson.put("productId", saleItem.productId)
                    siJson.put("productName", saleItem.productName)
                    siJson.put("quantity", saleItem.quantity)
                    siJson.put("costPrice", saleItem.costPrice)
                    siJson.put("sellingPrice", saleItem.sellingPrice)
                    saleItemArray.put(siJson)
                }
                jsonObject.put("sale_items", saleItemArray)

                onComplete(jsonObject.toString(4))
            } catch (e: Exception) {
                onComplete("")
            }
        }
    }

    fun importBackupJson(jsonString: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val jsonObject = org.json.JSONObject(jsonString)
                var restoredProductsCount = 0
                var restoredCustomersCount = 0
                var restoredSalesCount = 0
                var restoredSaleItemsCount = 0
                
                // Restore Products
                val productArray = jsonObject.optJSONArray("products")
                if (productArray != null) {
                    for (i in 0 until productArray.length()) {
                        val pJson = productArray.getJSONObject(i)
                        val id = pJson.optInt("id", 0)
                        val name = pJson.getString("name")
                        val costPrice = pJson.getDouble("costPrice")
                        val sellingPrice = pJson.getDouble("sellingPrice")
                        val stock = pJson.getInt("stock")
                        val category = pJson.optString("category", "General")
                        val minStockThreshold = pJson.optInt("minStockThreshold", 5)
                        val rating = pJson.optInt("rating", 5)
                        
                        repository.insertProduct(
                            Product(
                                id = id,
                                name = name,
                                costPrice = costPrice,
                                sellingPrice = sellingPrice,
                                stock = stock,
                                category = category,
                                minStockThreshold = minStockThreshold,
                                rating = rating
                            )
                        )
                        restoredProductsCount++
                    }
                }

                // Restore Customers
                val customerArray = jsonObject.optJSONArray("customers")
                if (customerArray != null) {
                    for (i in 0 until customerArray.length()) {
                        val cJson = customerArray.getJSONObject(i)
                        val id = cJson.optInt("id", 0)
                        val name = cJson.getString("name")
                        val phone = cJson.optString("phone", "")
                        val balance = cJson.optDouble("balance", 0.0)
                        val rating = cJson.optInt("rating", 5)
                        
                        repository.insertCustomer(
                            Customer(
                                id = id,
                                name = name,
                                phone = phone,
                                balance = balance,
                                rating = rating
                            )
                        )
                        restoredCustomersCount++
                    }
                }

                // Restore Sales
                val saleArray = jsonObject.optJSONArray("sales")
                if (saleArray != null) {
                    for (i in 0 until saleArray.length()) {
                        val sJson = saleArray.getJSONObject(i)
                        val id = sJson.optInt("id", 0)
                        val customerId = if (sJson.isNull("customerId")) null else sJson.getInt("customerId")
                        val customerName = if (sJson.isNull("customerName")) null else sJson.getString("customerName")
                        val timestamp = sJson.getLong("timestamp")
                        val totalPrice = sJson.getDouble("totalPrice")
                        val totalCost = sJson.getDouble("totalCost")

                        repository.insertSale(
                            Sale(
                                id = id,
                                customerId = customerId,
                                customerName = customerName,
                                timestamp = timestamp,
                                totalPrice = totalPrice,
                                totalCost = totalCost
                            )
                        )
                        restoredSalesCount++
                    }
                }

                // Restore SaleItems
                val saleItemArray = jsonObject.optJSONArray("sale_items")
                if (saleItemArray != null) {
                    for (i in 0 until saleItemArray.length()) {
                        val siJson = saleItemArray.getJSONObject(i)
                        val id = siJson.optInt("id", 0)
                        val saleId = siJson.getInt("saleId")
                        val productId = siJson.getInt("productId")
                        val productName = siJson.getString("productName")
                        val quantity = siJson.getInt("quantity")
                        val costPrice = siJson.getDouble("costPrice")
                        val sellingPrice = siJson.getDouble("sellingPrice")

                        repository.insertSaleItem(
                            com.example.data.database.SaleItem(
                                id = id,
                                saleId = saleId,
                                productId = productId,
                                productName = productName,
                                quantity = quantity,
                                costPrice = costPrice,
                                sellingPrice = sellingPrice
                            )
                        )
                        restoredSaleItemsCount++
                    }
                }
                
                onComplete(
                    true, 
                    "Mubarak ho! $restoredProductsCount Products, $restoredCustomersCount Customers, $restoredSalesCount Sales and $restoredSaleItemsCount Items successfully restored!"
                )
            } catch (e: Exception) {
                onComplete(false, "Restore failed: ${e.localizedMessage}")
            }
        }
    }

    fun getSaleItems(saleId: Int): kotlinx.coroutines.flow.Flow<List<com.example.data.database.SaleItem>> {
        return repository.getSaleItemsForSale(saleId)
    }
}

class DukanViewModelFactory(private val repository: DukanRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DukanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DukanViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
