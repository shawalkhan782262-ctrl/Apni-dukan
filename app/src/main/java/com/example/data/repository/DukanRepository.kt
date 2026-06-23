package com.example.data.repository

import com.example.data.database.*
import kotlinx.coroutines.flow.Flow

class DukanRepository(private val database: AppDatabase) {
    private val productDao = database.productDao()
    private val customerDao = database.customerDao()
    private val saleDao = database.saleDao()

    // Products
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()

    suspend fun insertProduct(product: Product) {
        productDao.insertProduct(product)
    }

    suspend fun updateProduct(product: Product) {
        productDao.updateProduct(product)
    }

    suspend fun deleteProduct(product: Product) {
        productDao.deleteProduct(product)
    }

    suspend fun updateProductStock(productId: Int, newStock: Int) {
        productDao.updateProductStock(productId, newStock)
    }

    // Customers
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()

    suspend fun insertCustomer(customer: Customer) {
        customerDao.insertCustomer(customer)
    }

    suspend fun updateCustomer(customer: Customer) {
        customerDao.updateCustomer(customer)
    }

    suspend fun deleteCustomer(customer: Customer) {
        customerDao.deleteCustomer(customer)
    }

    suspend fun updateCustomerBalance(customerId: Int, newBalance: Double) {
        customerDao.updateCustomerBalance(customerId, newBalance)
    }

    // Sales
    val allSales: Flow<List<Sale>> = saleDao.getAllSales()
    val allSaleItems: Flow<List<SaleItem>> = saleDao.getAllSaleItems()

    fun getSaleItemsForSale(saleId: Int): Flow<List<SaleItem>> {
        return saleDao.getSaleItemsForSale(saleId)
    }

    suspend fun insertSale(sale: Sale): Long {
        return saleDao.insertSale(sale)
    }

    suspend fun insertSaleItem(saleItem: SaleItem) {
        saleDao.insertSaleItem(saleItem)
    }

    /**
     * Executes a Sale transaction:
     * - Inserts the Sale record
     * - Inserts all SaleItems
     * - Deducts product stocks
     * - Updates customer credit balance (if sold on credit and customerId exists)
     */
    suspend fun executeSale(
        customerId: Int?,
        customerName: String?,
        isCreditSale: Boolean,
        items: List<Pair<Product, Int>> // Product & quantity sold
    ) {
        if (items.isEmpty()) return

        var totalPrice = 0.0
        var totalCost = 0.0

        for ((product, qty) in items) {
            totalPrice += product.sellingPrice * qty
            totalCost += product.costPrice * qty
        }

        // 1. Save main Sale record
        val saleId = saleDao.insertSale(
            Sale(
                customerId = customerId,
                customerName = customerName,
                totalPrice = totalPrice,
                totalCost = totalCost
            )
        )

        // 2. Save items & update stocks
        for ((product, qty) in items) {
            saleDao.insertSaleItem(
                SaleItem(
                    saleId = saleId.toInt(),
                    productId = product.id,
                    productName = product.name,
                    quantity = qty,
                    costPrice = product.costPrice,
                    sellingPrice = product.sellingPrice
                )
            )

            val updatedStock = maxOf(0, product.stock - qty)
            productDao.updateProductStock(product.id, updatedStock)
        }

        // 3. Update customer balance if credit sale
        if (customerId != null && isCreditSale) {
            adjustCustomerBalance(customerId, totalPrice)
        }
    }

    suspend fun adjustCustomerBalance(customerId: Int, amount: Double) {
        val customer = customerDao.getCustomerById(customerId)
        if (customer != null) {
            customerDao.updateCustomerBalance(customerId, customer.balance + amount)
        }
    }
}
