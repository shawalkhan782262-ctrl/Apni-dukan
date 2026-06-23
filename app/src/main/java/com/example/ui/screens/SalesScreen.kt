package com.example.ui.screens

import android.print.PrintAttributes
import android.print.PrintManager
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.platform.LocalContext
import java.io.Serializable
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.Customer
import com.example.data.database.Product
import com.example.data.database.Sale
import com.example.ui.viewmodel.DukanViewModel
import java.text.SimpleDateFormat
import java.util.*

import com.example.ui.components.QrScannerDialog
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.AttachMoney

data class FinalizedReceipt(
    val id: String = "INV-${System.currentTimeMillis().toString().takeLast(6)}",
    val customerName: String?,
    val customerPhone: String?,
    val customerRating: Int,
    val items: List<Pair<Product, Int>>,
    val isCredit: Boolean,
    val date: Long = System.currentTimeMillis(),
    val overallDiscount: Double = 0.0,
    val paymentMethod: String = "CASH",
    val mobileWalletNumber: String = ""
) : Serializable

enum class PaymentMethod(val label: String, val urduLabel: String, val color: Color, val icon: ImageVector) {
    CASH("Cash (Naqd)", "نقدی", Color(0xFF2E7D32), Icons.Default.Payments),
    CREDIT("Udhaar (Credit)", "ادھار", Color(0xFFC62828), Icons.Default.MoneyOff),
    EASYPAISA("Easypaisa", "ایزی پیسہ", Color(0xFF1B5E20), Icons.Default.PhoneAndroid),
    UPAISA("Upaisa", "یو پیسہ", Color(0xFFE65100), Icons.Default.AccountBalanceWallet)
}

@Composable
fun SalesScreen(
    viewModel: DukanViewModel,
    modifier: Modifier = Modifier
) {
    val products by viewModel.products.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val salesHistory by viewModel.sales.collectAsState()

    var activeTab by remember { mutableStateOf("new_sale") } // "new_sale" or "history"

    // Active sale states
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    val activeCart = remember { mutableStateListOf<Pair<Product, Int>>() } // Product & Qty

    var showProductPicker by remember { mutableStateOf(false) }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }
    var showOnlinePayDialog by remember { mutableStateOf(false) }
    var onlineWalletNumberInput by remember { mutableStateOf("") }
    var saleDiscountInput by remember { mutableStateOf("") }
    
    var editingCartItem by remember { mutableStateOf<Pair<Product, Int>?>(null) }
    var showPriceEditDialog by remember { mutableStateOf(false) }
    var finalizedReceipt by remember { mutableStateOf<FinalizedReceipt?>(null) }
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxSize()) {
        // Toggle tabs
        TabRow(
            selectedTabIndex = if (activeTab == "new_sale") 0 else 1,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = activeTab == "new_sale",
                onClick = { activeTab = "new_sale" },
                text = { Text("Nayi Bikri (New Sale)", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeTab == "history",
                onClick = { activeTab = "history" },
                text = { Text("Purana Record (History)", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (activeTab == "new_sale") {
            // NEW SALE ENTRY SCREEN
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Selector segment for Customer (Grahak)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Grahak aur Payment (Customer)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = selectedCustomer?.name ?: "Guest (Naqd Customer)",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Button(
                                onClick = { showCustomerPicker = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Tabdeel (Change)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Custom multi-mode cash/digital/credit payment profile selector
                        Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Payment Tareeqa (Payment Mode):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PaymentMethod.entries.forEach { method ->
                                    val isAllowed = method != PaymentMethod.CREDIT || selectedCustomer != null
                                    
                                    // Auto-fallback guest selector to CASH if CREDIT previously active
                                    if (selectedCustomer == null && paymentMethod == PaymentMethod.CREDIT) {
                                        paymentMethod = PaymentMethod.CASH
                                    }

                                    val isSelected = paymentMethod == method && isAllowed
                                    val containerColor = if (isSelected) method.color else MaterialTheme.colorScheme.surface
                                    val contentColor = if (isSelected) Color.White else if (isAllowed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) containerColor else if (isAllowed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f) else Color.LightGray.copy(alpha = 0.1f))
                                            .clickable(enabled = isAllowed) { 
                                                paymentMethod = method 
                                            }
                                            .border(
                                                1.dp, 
                                                if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), 
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(vertical = 8.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = method.icon,
                                                contentDescription = null,
                                                tint = if (isSelected) Color.White else if (isAllowed) method.color else Color.LightGray,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = method.urduLabel,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = contentColor
                                            )
                                            Text(
                                                text = method.label.substringBefore(" ("),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = contentColor,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Active billing items list (Cart)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sauda / active Cart (" + activeCart.size + " items)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { showProductPicker = true },
                            modifier = Modifier.testTag("add_item_to_cart_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Maal Daalein (+ Item)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Premium QR Code/Barcode Scanner Action Button
                        IconButton(
                            onClick = { showQrScanner = true },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(8.dp))
                                .size(40.dp)
                                .testTag("scan_qr_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan Barcode/QR Code",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                if (activeCart.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                            Text("Sauda khaali hai", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), fontWeight = FontWeight.Medium)
                            Text("Maal Daalein button par click kar k item shamil karain.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(activeCart) { cartPair ->
                            val (product, qty) = cartPair
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(product.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Rs. ${product.sellingPrice} x $qty", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Rs. ${product.sellingPrice * qty}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(end = 4.dp)
                                        )

                                        IconButton(
                                            onClick = {
                                                editingCartItem = cartPair
                                                showPriceEditDialog = true
                                            },
                                            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                                            modifier = Modifier.testTag("edit_cart_item_price_btn")
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Price/Qty")
                                        }

                                        IconButton(
                                            onClick = { activeCart.remove(cartPair) },
                                            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Bill Summary and complete sale action
                if (activeCart.isNotEmpty()) {
                    val subTotal = activeCart.sumOf { it.first.sellingPrice * it.second }
                    val directDiscount = saleDiscountInput.toDoubleOrNull() ?: 0.0
                    val grandTotal = maxOf(0.0, subTotal - directDiscount)

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Subtotal and Discount Input Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Subtotal (اصل بل):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                    Text("Rs. ${String.format("%,.0f", subTotal)}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }

                                // Discount text field (Compact Outlined)
                                OutlinedTextField(
                                    value = saleDiscountInput,
                                    onValueChange = { saleDiscountInput = it },
                                    label = { Text("Chhoot / Discount (Rs)", fontSize = 10.sp) },
                                    placeholder = { Text("0") },
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier
                                        .width(140.dp)
                                        .height(48.dp)
                                        .testTag("sale_discount_input")
                                )
                            }

                            Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Kul Qemat (Grand Total):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                    Text("Rs. ${String.format("%,.0f", grandTotal)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }

                                Button(
                                    onClick = {
                                        // Define the complete sales execution flow
                                        val onCompleteSaleAction: (String) -> Unit = { walletNum ->
                                            val itemsWithDiscount = if (directDiscount > 0.0 && activeCart.isNotEmpty()) {
                                                val totalOriginal = activeCart.sumOf { it.first.sellingPrice * it.second }
                                                if (totalOriginal > 0.0) {
                                                    activeCart.map { (product, qty) ->
                                                        val itemProportion = (product.sellingPrice * qty) / totalOriginal
                                                        val discountShare = directDiscount * itemProportion
                                                        val discountedPricePerUnit = maxOf(0.0, product.sellingPrice - (discountShare / qty))
                                                        Pair(product.copy(sellingPrice = discountedPricePerUnit), qty)
                                                    }
                                                } else activeCart.toList()
                                            } else {
                                                activeCart.toList()
                                            }

                                            finalizedReceipt = FinalizedReceipt(
                                                customerName = selectedCustomer?.name ?: "Guest Grahak",
                                                customerPhone = selectedCustomer?.phone ?: "",
                                                customerRating = selectedCustomer?.rating ?: 5,
                                                items = activeCart.toList().map { (product, qty) ->
                                                    val originalTotal = activeCart.sumOf { it.first.sellingPrice * it.second }
                                                    val portion = if (originalTotal > 0) (product.sellingPrice * qty) / originalTotal else 0.0
                                                    val unitDiscount = if (originalTotal > 0) (directDiscount * portion) / qty else 0.0
                                                    Pair(product.copy(sellingPrice = maxOf(0.0, product.sellingPrice - unitDiscount)), qty)
                                                },
                                                isCredit = (paymentMethod == PaymentMethod.CREDIT),
                                                overallDiscount = directDiscount,
                                                paymentMethod = paymentMethod.name,
                                                mobileWalletNumber = walletNum
                                            )

                                            viewModel.makeSale(
                                                customerId = selectedCustomer?.id,
                                                customerName = selectedCustomer?.name,
                                                isCreditSale = (paymentMethod == PaymentMethod.CREDIT),
                                                items = itemsWithDiscount
                                            )

                                            // Reset active billing variables
                                            activeCart.clear()
                                            selectedCustomer = null
                                            paymentMethod = PaymentMethod.CASH
                                            saleDiscountInput = ""
                                            onlineWalletNumberInput = ""
                                        }

                                        // Enforce online digital gateway checking
                                        if (paymentMethod == PaymentMethod.EASYPAISA || paymentMethod == PaymentMethod.UPAISA) {
                                            showOnlinePayDialog = true
                                        } else {
                                            // Execute directly for Cash or Udhaar
                                            onCompleteSaleAction("")
                                        }
                                    },
                                    modifier = Modifier.testTag("save_sale_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    enabled = activeCart.isNotEmpty()
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Bikri Mehfooz Karain", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // CRITICAL SALES JOURNAL / STATS HISTORICAL ARCHIVE
            if (salesHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(64.dp))
                        Text("Bikri ka koi record nahi mila", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Sales shuru karne k baad sari raseedain idhar record hon gi.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(salesHistory, key = { it.id }) { sale ->
                        val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                        val dateFormatted = dateFormat.format(Date(sale.timestamp))
                        val netProfit = sale.totalPrice - sale.totalCost

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Invoice #${sale.id}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Text(dateFormatted, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.secondaryContainer)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = sale.customerName ?: "Cash Guest",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }

                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Kul Bikri (Total Bill)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Rs. ${sale.totalPrice}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Bachat / Munafa (Profit)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = "Rs. $netProfit",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (netProfit >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
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

    // PRODUCT PICKER POP-UP DIALOG
    if (showProductPicker) {
        AlertDialog(
            onDismissRequest = { showProductPicker = false },
            title = { Text("Maal Choose Karain", fontWeight = FontWeight.Bold) },
            text = {
                val availableProducts = products.filter { it.stock > 0 }
                if (availableProducts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("Zameen par koi stock available nahi hai. Pehle product add karain ya buy back karain.", fontSize = 13.sp, color = Color.Red, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableProducts) { product ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .clickable {
                                        // Standard initial quantity of 1 added to active cart list
                                        val existingIndex = activeCart.indexOfFirst { it.first.id == product.id }
                                        if (existingIndex != -1) {
                                            val current = activeCart[existingIndex]
                                            if (current.second < product.stock) {
                                                activeCart[existingIndex] = Pair(current.first, current.second + 1)
                                            }
                                        } else {
                                            activeCart.add(Pair(product, 1))
                                        }
                                        showProductPicker = false
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(product.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Stock: ${product.stock} items available", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("Rs. ${product.sellingPrice}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showProductPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // CUSTOMER PICKER DIALOG
    if (showCustomerPicker) {
        AlertDialog(
            onDismissRequest = { showCustomerPicker = false },
            title = { Text("Grahak (Customer) Select Karain", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .clickable {
                                    selectedCustomer = null
                                    showCustomerPicker = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Guest Customer (Naqd Cash)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    if (customers.isEmpty()) {
                        item {
                            Text(
                                "No registered customers found. Add them inside Customers tab first.", 
                                fontSize = 12.sp, 
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(customers) { customer ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .clickable {
                                        selectedCustomer = customer
                                        showCustomerPicker = false
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                    Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                if (customer.balance > 0) {
                                    Text("Udhaar: Rs. ${customer.balance}", fontSize = 11.sp, color = Color(0xFFC62828), fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCustomerPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPriceEditDialog && editingCartItem != null) {
        val (product, qty) = editingCartItem!!
        var priceInput by remember { mutableStateOf(product.sellingPrice.toString()) }
        var qtyInput by remember { mutableStateOf(qty.toString()) }
        var priceError by remember { mutableStateOf(false) }
        var qtyError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { 
                showPriceEditDialog = false
                editingCartItem = null
            },
            title = {
                Column {
                    Text(
                        text = "Daam ya Tedaad Badlein (Rate Negotiator)",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            val originalProductInStock = products.find { it.id == product.id }
                            val originalPrice = originalProductInStock?.sellingPrice ?: product.sellingPrice
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Original Price (اصل ریٹ):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Rs. $originalPrice", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Kharid Qemat (Cost Price):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Rs. ${product.costPrice}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { 
                            priceInput = it 
                            val pFloat = it.toDoubleOrNull()
                            priceError = pFloat == null || pFloat < 0.0
                        },
                        label = { Text("Naya Bechnay ka Rate (Rs.)") },
                        isError = priceError,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        ),
                        singleLine = true,
                        leadingIcon = {
                            Text("Rs. ", fontWeight = FontWeight.Bold)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("edit_price_input")
                    )
                    if (priceError) {
                        Text("Please enter a valid price.", color = Color.Red, fontSize = 11.sp)
                    }

                    OutlinedTextField(
                        value = qtyInput,
                        onValueChange = { 
                            qtyInput = it
                            val qValue = it.toIntOrNull()
                            val stockAvailable = products.find { it.id == product.id }?.stock ?: 999999
                            qtyError = qValue == null || qValue <= 0 || qValue > stockAvailable
                        },
                        label = { Text("Tedaad (Quantity)") },
                        isError = qtyError,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_qty_input")
                    )
                    if (qtyError) {
                        val stockAvailable = products.find { it.id == product.id }?.stock ?: 0
                        Text("Ghalat Tedaad (Total Stock: $stockAvailable limit)", color = Color.Red, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newPrice = priceInput.toDoubleOrNull()
                        val newQty = qtyInput.toIntOrNull()
                        if (newPrice != null && newPrice >= 0.0 && newQty != null && newQty > 0) {
                            val index = activeCart.indexOf(editingCartItem!!)
                            if (index != -1) {
                                activeCart[index] = Pair(product.copy(sellingPrice = newPrice), newQty)
                            }
                            showPriceEditDialog = false
                            editingCartItem = null
                        }
                    },
                    enabled = !priceError && !qtyError && priceInput.isNotEmpty() && qtyInput.isNotEmpty(),
                    modifier = Modifier.testTag("apply_price_edit_button")
                ) {
                    Text("Mehfooz Karain (Save)")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPriceEditDialog = false
                        editingCartItem = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showQrScanner) {
        QrScannerDialog(
            availableProducts = products,
            onDismiss = { showQrScanner = false },
            onProductScanned = { product ->
                val existingIndex = activeCart.indexOfFirst { it.first.id == product.id }
                if (existingIndex != -1) {
                    val existing = activeCart[existingIndex]
                    activeCart[existingIndex] = Pair(existing.first, existing.second + 1)
                } else {
                    activeCart.add(Pair(product, 1))
                }
                showQrScanner = false
            }
        )
    }

    if (showOnlinePayDialog) {
        var walletError by remember { mutableStateOf("") }
        var isSimulatingPayment by remember { mutableStateOf(false) }
        var paymentSecondsRemaining by remember { mutableStateOf(0) }

        LaunchedEffect(isSimulatingPayment) {
            if (isSimulatingPayment) {
                paymentSecondsRemaining = 3
                while (paymentSecondsRemaining > 0) {
                    kotlinx.coroutines.delay(1000)
                    paymentSecondsRemaining--
                }
                
                val subTotal = activeCart.sumOf { it.first.sellingPrice * it.second }
                val directDiscount = saleDiscountInput.toDoubleOrNull() ?: 0.0
                
                val itemsWithDiscount = if (directDiscount > 0.0 && activeCart.isNotEmpty()) {
                    val totalOriginal = activeCart.sumOf { it.first.sellingPrice * it.second }
                    if (totalOriginal > 0.0) {
                        activeCart.map { (product, qty) ->
                            val itemProportion = (product.sellingPrice * qty) / totalOriginal
                            val discountShare = directDiscount * itemProportion
                            val discountedPricePerUnit = maxOf(0.0, product.sellingPrice - (discountShare / qty))
                            Pair(product.copy(sellingPrice = discountedPricePerUnit), qty)
                        }
                    } else activeCart.toList()
                } else {
                    activeCart.toList()
                }

                finalizedReceipt = FinalizedReceipt(
                    customerName = selectedCustomer?.name ?: "Guest Grahak",
                    customerPhone = selectedCustomer?.phone ?: "",
                    customerRating = selectedCustomer?.rating ?: 5,
                    items = activeCart.toList().map { (product, qty) ->
                        val originalTotal = activeCart.sumOf { it.first.sellingPrice * it.second }
                        val portion = if (originalTotal > 0) (product.sellingPrice * qty) / originalTotal else 0.0
                        val unitDiscount = if (originalTotal > 0) (directDiscount * portion) / qty else 0.0
                        Pair(product.copy(sellingPrice = maxOf(0.0, product.sellingPrice - unitDiscount)), qty)
                    },
                    isCredit = false,
                    overallDiscount = directDiscount,
                    paymentMethod = paymentMethod.name,
                    mobileWalletNumber = onlineWalletNumberInput
                )

                viewModel.makeSale(
                    customerId = selectedCustomer?.id,
                    customerName = selectedCustomer?.name,
                    isCreditSale = false,
                    items = itemsWithDiscount
                )

                // Clear fields
                activeCart.clear()
                selectedCustomer = null
                paymentMethod = PaymentMethod.CASH
                saleDiscountInput = ""
                onlineWalletNumberInput = ""
                showOnlinePayDialog = false
                isSimulatingPayment = false
            }
        }

        AlertDialog(
            onDismissRequest = { if (!isSimulatingPayment) showOnlinePayDialog = false },
            title = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = paymentMethod.icon,
                        contentDescription = null,
                        tint = paymentMethod.color,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "${paymentMethod.label} Mobile Wallet",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            text = {
                val subTotal = activeCart.sumOf { it.first.sellingPrice * it.second }
                val directDiscount = saleDiscountInput.toDoubleOrNull() ?: 0.0
                val grandTotal = maxOf(0.0, subTotal - directDiscount)
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Total Billing Amount: Rs. ${String.format("%,.0f", grandTotal)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = paymentMethod.color
                    )

                    Text(
                        text = "Enter customer's 11-digit ${paymentMethod.label} mobile account number below to trigger standard payment prompt:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = onlineWalletNumberInput,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() } && it.length <= 11) {
                                onlineWalletNumberInput = it
                                walletError = ""
                            }
                        },
                        label = { Text("Account Phone Number (e.g., 03001234567)") },
                        placeholder = { Text("03xxxxxxxxx") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("wallet_phone_input_field"),
                        enabled = !isSimulatingPayment
                    )

                    if (walletError.isNotEmpty()) {
                        Text(walletError, color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = paymentMethod.color.copy(alpha = 0.05f)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "Or scan shop dynamic payment QR Code:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = paymentMethod.color
                            )
                            
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .background(Color.White, RoundedCornerShape(8.dp))
                                    .border(1.5.dp, paymentMethod.color, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = "Dynamic QR payment code",
                                    tint = paymentMethod.color,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            
                            Text(
                                "QR value: pak-pay://${paymentMethod.name.lowercase()}?amt=$grandTotal&acc=03158380421",
                                fontSize = 8.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if (isSimulatingPayment) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(color = paymentMethod.color)
                            Text(
                                text = "Awaiting customer approval pin... (${paymentSecondsRemaining}s)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = paymentMethod.color
                            )
                        }
                    }
                }
            },
            confirmButton = {
                val subTotal = activeCart.sumOf { it.first.sellingPrice * it.second }
                val directDiscount = saleDiscountInput.toDoubleOrNull() ?: 0.0
                if (!isSimulatingPayment) {
                    Button(
                        onClick = {
                            if (onlineWalletNumberInput.length != 11 || !onlineWalletNumberInput.startsWith("0")) {
                                walletError = "Meherbani kar k 11-digit ka valid mobile number enter karain jo '0' se shuru ho."
                            } else {
                                isSimulatingPayment = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = paymentMethod.color),
                        modifier = Modifier.testTag("submit_online_pay_btn")
                    ) {
                        Text("Confirm and Collect Rs. ${String.format("%.0f", subTotal - directDiscount)}")
                    }
                }
            },
            dismissButton = {
                if (!isSimulatingPayment) {
                    TextButton(onClick = { showOnlinePayDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (finalizedReceipt != null) {
        ReceiptDialog(
            receipt = finalizedReceipt!!,
            onDismiss = { finalizedReceipt = null }
        )
    }
}

@Composable
fun ReceiptDialog(
    receipt: FinalizedReceipt,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bikri Raseed (Finalized Invoice)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("receipt_close_btn")) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF9F9F9),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "APNI DUKAN",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Created by Shawal FF",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "★ RAFAQAT & BHAROSA ★",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF388E3C),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "-".repeat(34),
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodySmall
                    )

                    // Invoice details
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("ID:", fontSize = 11.sp, color = Color.Gray)
                            Text(receipt.id, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tareekh (Date):", fontSize = 11.sp, color = Color.Gray)
                            val df = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault())
                            Text(df.format(Date(receipt.date)), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Grahak (Client):", fontSize = 11.sp, color = Color.Gray)
                            Text(receipt.customerName ?: "Guest (Cash)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Star rating trust display
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Grahak Trust:", fontSize = 11.sp, color = Color.Gray)
                            Row {
                                (1..5).forEach { index ->
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (index <= receipt.customerRating) Color(0xFFFFB300) else Color.LightGray,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "-".repeat(34),
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodySmall
                    )

                    // Item list headers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Maal / Rate x Qty", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray, modifier = Modifier.weight(1f))
                        Text("Total Price", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray, textAlign = TextAlign.End)
                    }

                    // Render sold items
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        receipt.items.forEach { (product, qty) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(product.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Rs. ${product.sellingPrice} per unit", fontSize = 10.sp, color = Color.Gray)
                                }
                                Text(
                                    text = "x$qty",
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    color = Color.DarkGray
                                )
                                Text(
                                    text = "Rs. ${String.format("%,.0f", product.sellingPrice * qty)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.DarkGray,
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }

                    Text(
                        text = "-".repeat(34),
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodySmall
                    )

                    // Totals
                    val subtotal = receipt.items.sumOf { it.first.sellingPrice * it.second } + receipt.overallDiscount
                    val grandTotal = maxOf(0.0, subtotal - receipt.overallDiscount)

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal:", fontSize = 12.sp, color = Color.Gray)
                            Text("Rs. ${String.format("%,.0f", subtotal)}", fontSize = 12.sp)
                        }
                        if (receipt.overallDiscount > 0.0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Chhoot / Discount:", fontSize = 12.sp, color = Color(0xFF388E3C), fontWeight = FontWeight.Bold)
                                Text("-Rs. ${String.format("%,.0f", receipt.overallDiscount)}", fontSize = 12.sp, color = Color(0xFF388E3C), fontWeight = FontWeight.Bold)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("GRAND TOTAL:", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Rs. ${String.format("%,.0f", grandTotal)}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Bikri Tareeqa (Method):", fontSize = 11.sp, color = Color.Gray)
                            val methodText = when (receipt.paymentMethod) {
                                "CREDIT" -> "UDHAAR / CREDIT ⏳"
                                "EASYPAISA" -> "EASYPAISA ONLINE PAID ✅"
                                "UPAISA" -> "UPAISA MOBILE PAID ✅"
                                else -> "NAQD / CASH PAID ✅"
                            }
                            val methodColor = when (receipt.paymentMethod) {
                                "CREDIT" -> Color(0xFFD32F2F)
                                "EASYPAISA" -> Color(0xFF1B5E20)
                                "UPAISA" -> Color(0xFFE65100)
                                else -> Color(0xFF388E3C)
                            }
                            Text(
                                text = methodText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = methodColor
                            )
                        }
                        if (receipt.mobileWalletNumber.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Wallet Account:", fontSize = 11.sp, color = Color.Gray)
                                Text(receipt.mobileWalletNumber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text(
                        text = "* Shukriya! Dobara Tashreef Laiyen! *",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val htmlContent = generateReceiptHtml(receipt)
                    printReceipt(context, htmlContent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("receipt_print_btn")
            ) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Print", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Button(
                onClick = {
                    shareReceiptText(context, receipt)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("receipt_share_btn")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Share (WhatsApp)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    )
}

fun printReceipt(context: Context, htmlContent: String) {
    try {
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val printAdapter = webView.createPrintDocumentAdapter("Shawal_FF_Dukan_Receipt")
                printManager.print(
                    "Apni Dukan Receipt",
                    printAdapter,
                    PrintAttributes.Builder().build()
                )
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun shareReceiptText(context: Context, receipt: FinalizedReceipt) {
    val df = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault())
    val dateStr = df.format(Date(receipt.date))
    val itemsStr = receipt.items.joinToString("\n") { (p, qty) ->
        "• ${p.name} (x$qty) - Rs. ${String.format("%.0f", p.sellingPrice * qty)}"
    }
    val creditStr = when (receipt.paymentMethod) {
        "CREDIT" -> "UDHAAR / CREDIT ⏳"
        "EASYPAISA" -> "EASYPAISA MOBILE PAID ✅ (Acc: ${receipt.mobileWalletNumber})"
        "UPAISA" -> "UPAISA MOBILE PAID ✅ (Acc: ${receipt.mobileWalletNumber})"
        else -> "NAQD / CASH PAID ✅"
    }
    val subtotal = receipt.items.sumOf { it.first.sellingPrice * it.second } + receipt.overallDiscount
    val total = maxOf(0.0, subtotal - receipt.overallDiscount)
    val stars = "★".repeat(receipt.customerRating) + "☆".repeat(5 - receipt.customerRating)

    val msg = """
======= APNI DUKAN =======
      (Created by Shawal FF)
Receipt ID: ${receipt.id}
Date: $dateStr
Customer: ${receipt.customerName ?: "Cash Guest"}
Customer Rating: $stars
------------------------
$itemsStr
------------------------
Subtotal: Rs. ${String.format("%.0f", subtotal)}
Discount: Rs. ${String.format("%.0f", receipt.overallDiscount)}
GRAND TOTAL: Rs. ${String.format("%.0f", total)}
Payment Mode: $creditStr

Shukriya! Dobara tashreef laiyega!
==========================
    """.trimIndent()

    val sendIntent = android.content.Intent().apply {
        action = android.content.Intent.ACTION_SEND
        putExtra(android.content.Intent.EXTRA_TEXT, msg)
        type = "text/plain"
    }
    val shareIntent = android.content.Intent.createChooser(sendIntent, "Raseed Bhejayen (Share Invoice)")
    context.startActivity(shareIntent)
}

fun generateReceiptHtml(receipt: FinalizedReceipt): String {
    val df = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault())
    val dateStr = df.format(Date(receipt.date))
    val subtotal = receipt.items.sumOf { it.first.sellingPrice * it.second } + receipt.overallDiscount
    val total = maxOf(0.0, subtotal - receipt.overallDiscount)
    val stars = "★".repeat(receipt.customerRating) + "☆".repeat(5 - receipt.customerRating)

    val rowsHtml = receipt.items.joinToString("") { (p, qty) ->
        """
        <tr>
            <td style="padding: 4px 0; text-align: left;">${p.name}<br/><span style="font-size: 10px; color: #555;">Rs. ${p.sellingPrice} per unit</span></td>
            <td style="padding: 4px 0; text-align: center;">x$qty</td>
            <td style="padding: 4px 0; text-align: right;">Rs. ${String.format("%.0f", p.sellingPrice * qty)}</td>
        </tr>
        """
    }

    val paymentModeHtmlText = when (receipt.paymentMethod) {
        "CREDIT" -> "UDHAAR / CREDIT ⏳"
        "EASYPAISA" -> "EASYPAISA MOBILE PAID ✅ (${receipt.mobileWalletNumber})"
        "UPAISA" -> "UPAISA MOBILE PAID ✅ (${receipt.mobileWalletNumber})"
        else -> "CASH / PAID ✅"
    }

    return """
    <html>
    <head>
    <style>
        body { font-family: 'Courier New', Courier, monospace; margin: 20px; color: #000; }
        .receipt-card { max-width: 320px; margin: auto; padding: 10px; border: 1px dashed #888; }
        .center { text-align: center; }
        .right { text-align: right; }
        .bold { font-weight: bold; }
        table { width: 100%; border-collapse: collapse; margin: 10px 0; }
        hr { border-top: 1px dashed #000; border-bottom: none; }
        .stars { color: #FFA000; font-size: 18px; }
    </style>
    </head>
    <body>
    <div class="receipt-card">
        <h2 class="center bold" style="margin-bottom: 2px;">APNI DUKAN</h2>
        <p class="center" style="font-size: 11px; margin: 0 0 5px 0;">Created by Shawal FF</p>
        <p class="center bold" style="font-size: 11px; letter-spacing: 2px; margin: 0;">★ PREMIUM RECEIPT ★</p>
        <hr/>
        <p style="font-size: 12px; margin: 4px 0;"><b>Receipt ID:</b> ${receipt.id}</p>
        <p style="font-size: 12px; margin: 4px 0;"><b>Date:</b> $dateStr</p>
        <p style="font-size: 12px; margin: 4px 0;"><b>Customer:</b> ${receipt.customerName ?: "Cash Guest"}</p>
        <p style="font-size: 12px; margin: 4px 0;"><b>Trust Score:</b> <span class="stars">$stars</span></p>
        <hr/>
        <table>
            <thead>
                <tr style="border-bottom: 1px dashed #000;">
                    <th style="text-align: left; font-size: 11px;">Item</th>
                    <th style="text-align: center; font-size: 11px;">Qty</th>
                    <th style="text-align: right; font-size: 11px;">Price</th>
                </tr>
            </thead>
            <tbody>
                $rowsHtml
            </tbody>
        </table>
        <hr/>
        <p style="font-size: 13px; margin: 4px 0;" class="right">Subtotal: Rs. ${String.format("%.0f", subtotal)}</p>
        <p style="font-size: 13px; margin: 4px 0;" class="right">Discount: Rs. ${String.format("%.0f", receipt.overallDiscount)}</p>
        <hr/>
        <h3 style="margin: 6px 0;" class="right bold">GRAND TOTAL: Rs. ${String.format("%.0f", total)}</h3>
        <p style="font-size: 12px; margin: 4px 0;" class="right"><b>Payment Mode:</b> $paymentModeHtmlText</p>
        <hr/>
        <p class="center" style="font-size: 11px; margin-top: 15px;">Shukriya! Dobara tashreef laiyega.</p>
        <p class="center" style="font-size: 9px; color: #777;">Powered by Apni Dukan Store Engine</p>
    </div>
    </body>
    </html>
    """
}
