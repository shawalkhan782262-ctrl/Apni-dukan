package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val costPrice: Double,     // Kharid Qemat (Price at which owner bought item)
    val sellingPrice: Double,  // Bechnay ki Qemat (Price at which owner sells item)
    val stock: Int,            // Kitni items available hain
    val category: String = "General",
    val minStockThreshold: Int = 5 // Minimum stock threshold before alerting
) : Serializable

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String = "",
    val balance: Double = 0.0,   // Negative balance means Udhaar (credit), positive means advance
    val rating: Int = 5          // Customer rating / trust score out of 5 stars
) : Serializable

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerId: Int? = null,
    val customerName: String? = null, // Cached for historical accuracy if customer deleted
    val timestamp: Long = System.currentTimeMillis(),
    val totalPrice: Double,
    val totalCost: Double      // Sum of (costPrice * quantity) for profit/loss calculation
) : Serializable

@Entity(tableName = "sale_items")
data class SaleItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val saleId: Int,
    val productId: Int,
    val productName: String,
    val quantity: Int,
    val costPrice: Double,     // Historic cost price at sale time
    val sellingPrice: Double   // Historic selling price at sale time
) : Serializable
