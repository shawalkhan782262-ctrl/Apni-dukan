package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.Product
import com.example.ui.viewmodel.DukanViewModel
import com.example.ui.components.InventoryTableComponent
import androidx.compose.foundation.BorderStroke
import com.example.ui.components.RestockLocatorBottomSheet
import com.example.ui.components.LocalInventoryComponent

import androidx.compose.ui.platform.LocalContext

@Composable
fun ProductsScreen(
    viewModel: DukanViewModel,
    modifier: Modifier = Modifier
) {
    val products by viewModel.products.collectAsState()
    var isLocalMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedProductForEdit by remember { mutableStateOf<Product?>(null) }
    var qrCodeForProduct by remember { mutableStateOf<Product?>(null) }
    var showSearchScanner by remember { mutableStateOf(false) }
    var isTableView by remember { mutableStateOf(true) }

    // Alert system state variables
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("dukan_inventory_alert_prefs", android.content.Context.MODE_PRIVATE) }
    var lowStockThreshold by remember { mutableStateOf(sharedPrefs.getInt("low_stock_threshold", 5)) }
    var showOnlyLowStock by remember { mutableStateOf(false) }
    var showRestockLocator by remember { mutableStateOf(false) }

    val lowStockCount = remember(products) {
        products.count { it.stock <= it.minStockThreshold }
    }

    val filteredProducts = remember(products, searchQuery, showOnlyLowStock) {
        products.filter { 
            val matchesSearch = it.name.contains(searchQuery, ignoreCase = true)
            val matchesLowStock = !showOnlyLowStock || it.stock <= it.minStockThreshold
            matchesSearch && matchesLowStock
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Mode Tab Switcher: Store DB vs. Local Scratchpad
            TabRow(
                selectedTabIndex = if (isLocalMode) 1 else 0,
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            ) {
                Tab(
                    selected = !isLocalMode,
                    onClick = { isLocalMode = false },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Store Inventory", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("ڈیٹا بیس اسٹاک", fontSize = 9.sp)
                        }
                    },
                    icon = { Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_store_db")
                )
                Tab(
                    selected = isLocalMode,
                    onClick = { isLocalMode = true },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Local State Draft", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("عارضی کھاتا", fontSize = 9.sp)
                        }
                    },
                    icon = { Icon(Icons.Default.SettingsSuggest, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_local_state")
                )
            }

            if (isLocalMode) {
                LocalInventoryComponent(modifier = Modifier.weight(1f))
            } else {
                // Search Bar & Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Item search karain... (مثال: چاول)") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("product_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // QR Scanner Button next to search bar
                IconButton(
                    onClick = { showSearchScanner = true },
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .testTag("qr_search_scanner_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan Product",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                // Layout view toggler: Grid/Table Switcher
                IconButton(
                    onClick = { isTableView = !isTableView },
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .testTag("layout_toggle_btn")
                ) {
                    Icon(
                        imageVector = if (isTableView) Icons.Default.GridView else Icons.Default.TableChart,
                        contentDescription = "Toggle Layout",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Clean Dynamic Alert panel & threshold control options 
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (lowStockCount > 0) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                ),
                border = BorderStroke(
                    1.dp, 
                    if (lowStockCount > 0) Color(0xFFEF9A9A) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("low_stock_control_panel")
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (lowStockCount > 0) Icons.Default.Warning else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (lowStockCount > 0) Color(0xFFD32F2F) else Color(0xFF2E7D32),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (lowStockCount > 0) {
                                    "$lowStockCount Items Below Threshold! / کم اسٹاک الرٹ"
                                } else {
                                    "All items fully stocked / اسٹاک کلیئر ہے"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (lowStockCount > 0) Color(0xFFD32F2F) else Color(0xFF2E7D32)
                            )
                        }

                        // Checkbox filter to see only low stock items
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.clickable { showOnlyLowStock = !showOnlyLowStock }
                        ) {
                            Text(
                                "Show Low Only",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Checkbox(
                                checked = showOnlyLowStock,
                                onCheckedChange = { showOnlyLowStock = it },
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("filter_low_stock_checkbox")
                            )
                        }
                    }

                    if (lowStockCount > 0) {
                        Text(
                            text = "ان مصنوعات کا اسٹاک ہول سیلر سے پورا کریں تاکہ کاروبار نہ رکے۔",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFC62828)
                        )
                    }

                    Divider(color = (if (lowStockCount > 0) Color(0xFFEF9A9A) else MaterialTheme.colorScheme.outlineVariant).copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: alert threshold adjuster
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Alert Limit:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            IconButton(
                                onClick = { 
                                    if (lowStockThreshold > 1) {
                                        lowStockThreshold--
                                        sharedPrefs.edit().putInt("low_stock_threshold", lowStockThreshold).apply()
                                    }
                                },
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                    .testTag("btn_threshold_dec")
                            ) {
                                Icon(Icons.Default.Remove, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                            Text(
                                text = "$lowStockThreshold Items",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = { 
                                    lowStockThreshold++
                                    sharedPrefs.edit().putInt("low_stock_threshold", lowStockThreshold).apply()
                                },
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                    .testTag("btn_threshold_inc")
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        // Right: Supplier Locator map finder
                        Button(
                            onClick = { showRestockLocator = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (lowStockCount > 0) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(28.dp)
                                .testTag("btn_find_supplier")
                        ) {
                            Icon(Icons.Default.Map, null, modifier = Modifier.size(12.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Wholesale Locator", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // Products List
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = if (searchQuery.isEmpty()) "Kuch maal mojood nahi hai" else "Maching item nahi mili",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (searchQuery.isEmpty()) "Inventory mein nayi items shamil karne k liye niche diye gaye button '+' par click karain" else "Koi aur naam type kar k dekhain.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                if (isTableView) {
                    InventoryTableComponent(
                        products = filteredProducts,
                        onIncrementStock = { product, amount ->
                            viewModel.updateProduct(product.copy(stock = maxOf(0, product.stock + amount)))
                        },
                        onEditProduct = { selectedProductForEdit = it },
                        onDeleteProduct = { viewModel.deleteProduct(it) },
                        onGenerateQrCode = { qrCodeForProduct = it },
                        lowStockThreshold = lowStockThreshold,
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 80.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(filteredProducts, key = { it.id }) { product ->
                            ProductItemCard(
                                product = product,
                                onEditClick = { selectedProductForEdit = product },
                                onQrClick = { qrCodeForProduct = product },
                                lowStockThreshold = lowStockThreshold
                            )
                        }
                    }
                }
            }
            }
        }

        // Floating Action Button to Add Product
        if (!isLocalMode) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .testTag("add_product_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Item shamil karay")
            }
        }
    }

    // Add Product Dialog
    if (showAddDialog) {
        ProductDialog(
            title = "Naya Maal / Add Product",
            onDismiss = { showAddDialog = false },
            onSave = { name, cost, sell, stock, cat, thresh ->
                viewModel.addProduct(name, cost, sell, stock, cat, thresh)
                showAddDialog = false
            }
        )
    }

    // Edit Product Dialog
    selectedProductForEdit?.let { product ->
        ProductDialog(
            title = "Maal Edit Karain / Edit Product",
            product = product,
            onDismiss = { selectedProductForEdit = null },
            onSave = { name, cost, sell, stock, cat, thresh ->
                viewModel.updateProduct(
                    product.copy(
                        name = name,
                        costPrice = cost,
                        sellingPrice = sell,
                        stock = stock,
                        category = cat,
                        minStockThreshold = thresh
                    )
                )
                selectedProductForEdit = null
            },
            onDelete = {
                viewModel.deleteProduct(product)
                selectedProductForEdit = null
            }
        )
    }

    if (showRestockLocator) {
        RestockLocatorBottomSheet(
            onDismissRequest = { showRestockLocator = false }
        )
    }

    if (showSearchScanner) {
        com.example.ui.components.QrScannerDialog(
            availableProducts = products,
            onDismiss = { showSearchScanner = false },
            onProductScanned = { scanned ->
                selectedProductForEdit = scanned
                showSearchScanner = false
            }
        )
    }

    qrCodeForProduct?.let { product ->
        com.example.ui.components.ProductQrCodeDialog(
            product = product,
            onDismiss = { qrCodeForProduct = null }
        )
    }
}

@Composable
fun ProductItemCard(
    product: Product,
    onEditClick: () -> Unit,
    onQrClick: () -> Unit,
    lowStockThreshold: Int = 5,
    modifier: Modifier = Modifier
) {
    val isLow = product.stock <= product.minStockThreshold
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onEditClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLow) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surface
        ),
        border = if (isLow) BorderStroke(1.5.dp, Color(0xFFEF9A9A)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    SuggestionChip(
                        onClick = {},
                        label = { Text(product.category, fontSize = 10.sp) },
                        modifier = Modifier.height(20.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { onQrClick() },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("qr_card_toggle_btn_${product.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "Show item QR Code",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ColumnValueLabel(label = "Kharid (Cost)", value = "Rs. ${product.costPrice}")
                    ColumnValueLabel(label = "Bechna (Sell)", value = "Rs. ${product.sellingPrice}")

                    val profitMargin = product.sellingPrice - product.costPrice
                    ColumnValueLabel(
                        label = "Munafa (Margin)",
                        value = "Rs. $profitMargin",
                        valueColor = if (profitMargin >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
            }

            // Stock badge on the right
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val stockColor = when {
                    product.stock == 0 -> Color(0xFFC62828)
                    product.stock <= product.minStockThreshold -> Color(0xFFD32F2F)
                    else -> Color(0xFF2E7D32)
                }
                val stockBgColor = stockColor.copy(alpha = 0.12f)

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(stockBgColor)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (product.stock == 0) "Khatam (Out)" else "In Stock: ${product.stock}",
                        color = stockColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
                Text(
                    text = "Mojooda Maal",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ColumnValueLabel(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column {
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDialog(
    title: String,
    product: Product? = null,
    onDismiss: () -> Unit,
    onSave: (String, Double, Double, Int, String, Int) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var costPrice by remember { mutableStateOf(product?.costPrice?.toString() ?: "") }
    var sellingPrice by remember { mutableStateOf(product?.sellingPrice?.toString() ?: "") }
    var stock by remember { mutableStateOf(product?.stock?.toString() ?: "") }
    var category by remember { mutableStateOf(product?.category ?: "General") }
    var minStockThreshold by remember { mutableStateOf(product?.minStockThreshold?.toString() ?: "5") }

    var hasError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item ka Naam (Product Name)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Zat / Category") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = stock,
                        onValueChange = { stock = it },
                        label = { Text("Stock") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = costPrice,
                        onValueChange = { costPrice = it },
                        label = { Text("Kharid (Cost Price)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = sellingPrice,
                        onValueChange = { sellingPrice = it },
                        label = { Text("Farokht (Sale Price)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = minStockThreshold,
                    onValueChange = { minStockThreshold = it },
                    label = { Text("Alert Limit (Kam se Kam Stock Alert)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (hasError) {
                    Text(
                        text = "Meherbani kar k sari details durust add karay.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cost = costPrice.toDoubleOrNull()
                    val sell = sellingPrice.toDoubleOrNull()
                    val stk = stock.toIntOrNull()
                    val thresh = minStockThreshold.toIntOrNull() ?: 5
                    if (name.isNotEmpty() && cost != null && sell != null && stk != null && thresh >= 0) {
                        onSave(name, cost, sell, stk, category, thresh)
                    } else {
                        hasError = true
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                }
            }
        }
    )
}
