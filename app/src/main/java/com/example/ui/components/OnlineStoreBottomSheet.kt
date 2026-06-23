package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.Product
import com.example.ui.viewmodel.DukanViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineStoreBottomSheet(
    onDismissRequest: () -> Unit,
    viewModel: DukanViewModel
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val products by viewModel.products.collectAsState()
    
    // Tab setup
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Online Orders / آرڈرز", "Digital Catalog / شوکیس", "Store Settings / سیٹنگز")
    val tabIcons = listOf(Icons.Default.ShoppingCart, Icons.Default.Storefront, Icons.Default.Settings)

    // Store Settings States
    val sharedPrefs = remember { context.getSharedPreferences("dukan_online_store_prefs", Context.MODE_PRIVATE) }
    var storeName by remember { mutableStateOf(sharedPrefs.getString("store_name", "Shawal Premium Store") ?: "Shawal Premium Store") }
    var storeDesc by remember { mutableStateOf(sharedPrefs.getString("store_desc", "Mobiles, Karyana and Premium Electronics Items under one roof.") ?: "Mobiles, Karyana and Premium Electronics Items under one roof.") }
    var whatsappNum by remember { mutableStateOf(sharedPrefs.getString("store_whatsapp", "03001234567") ?: "03001234567") }
    var isLive by remember { mutableStateOf(sharedPrefs.getBoolean("store_is_live", true)) }

    // Online Products catalog states (persisted as set of IDs in SP)
    var onlineProductIds by remember {
        mutableStateOf<Set<String>>(
            sharedPrefs.getStringSet("online_product_ids", emptySet())?.toSet() ?: emptySet()
        )
    }

    // Web Orders state
    val ordersListKey = "online_orders_list_json"
    var ordersListJson by remember {
        mutableStateOf(
            sharedPrefs.getString(ordersListKey, "") ?: ""
        )
    }

    // Helper to load/save manual orders list
    val onlineOrders = remember(ordersListJson) {
        val list = mutableStateListOf<OnlineOrder>()
        if (ordersListJson.isNotEmpty()) {
            try {
                val array = JSONArray(ordersListJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val itemsList = mutableListOf<OrderItem>()
                    val itemsArray = obj.getJSONArray("items")
                    for (j in 0 until itemsArray.length()) {
                        val itemObj = itemsArray.getJSONObject(j)
                        itemsList.add(
                            OrderItem(
                                productName = itemObj.getString("name"),
                                qty = itemObj.getInt("qty"),
                                price = itemObj.getDouble("price")
                            )
                        )
                    }
                    list.add(
                        OnlineOrder(
                            id = obj.getString("id"),
                            customerName = obj.getString("customerName"),
                            phone = obj.getString("phone"),
                            timestamp = obj.getLong("timestamp"),
                            status = obj.getString("status"),
                            items = itemsList,
                            total = obj.getDouble("total"),
                            orderType = obj.optString("orderType", "WhatsApp App")
                        )
                    )
                }
            } catch (e: Exception) {
                // Return defaults if parsing failed
            }
        }
        if (list.isEmpty()) {
            // Seed defaults
            list.addAll(
                listOf(
                    OnlineOrder(
                        id = "#1092",
                        customerName = "Mian Arslan",
                        phone = "03219876543",
                        timestamp = System.currentTimeMillis() - 7200000,
                        status = "Pending ⏳",
                        items = listOf(OrderItem("Mobile Charger (Fast)", 1, 850.0)),
                        total = 850.0,
                        orderType = "Web Catalog"
                    ),
                    OnlineOrder(
                        id = "#1091",
                        customerName = "Ayesha Malik",
                        phone = "03124567890",
                        timestamp = System.currentTimeMillis() - 36000000,
                        status = "Packing 📦",
                        items = listOf(OrderItem("LED Bulb 12W", 3, 240.0), OrderItem("Sufi Cooking Oil 1L", 1, 560.0)),
                        total = 1280.0,
                        orderType = "WhatsApp Direct"
                    ),
                    OnlineOrder(
                        id = "#1090",
                        customerName = "Muhammad Zain",
                        phone = "03335552211",
                        timestamp = System.currentTimeMillis() - 86400000,
                        status = "Delivered ✅",
                        items = listOf(OrderItem("Surf Excel 500g", 2, 310.0)),
                        total = 620.0,
                        orderType = "Facebook Ads"
                    )
                )
            )
        }
        list
    }

    fun saveOrders() {
        try {
            val array = JSONArray()
            for (order in onlineOrders) {
                val obj = JSONObject()
                obj.put("id", order.id)
                obj.put("customerName", order.customerName)
                obj.put("phone", order.phone)
                obj.put("timestamp", order.timestamp)
                obj.put("status", order.status)
                obj.put("total", order.total)
                obj.put("orderType", order.orderType)

                val itemsArray = JSONArray()
                for (item in order.items) {
                    val itemObj = JSONObject()
                    itemObj.put("name", item.productName)
                    itemObj.put("qty", item.qty)
                    itemObj.put("price", item.price)
                    itemsArray.put(itemObj)
                }
                obj.put("items", itemsArray)
                array.put(obj)
            }
            ordersListJson = array.toString()
            sharedPrefs.edit().putString(ordersListKey, ordersListJson).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    var showAddManualOrderDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier
            .fillMaxHeight(0.92f)
            .testTag("online_store_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Online Dukan System (آن لائن دکان)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isLive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isLive) "LIVE 🟢" else "OFFLINE 🔴",
                                color = if (isLive) Color(0xFF2E7D32) else Color(0xFFC62828),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Customize digital store catalog, WhatsApp checkout & process web orders.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(Icons.Default.Close, contentDescription = "Close sheet")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(tabIcons[index], contentDescription = null, modifier = Modifier.size(18.dp)) },
                        text = {
                            Text(
                                text = title.split(" / ").lastOrNull() ?: title,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tab Content Panes
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    0 -> { // TAB 0: ONLINE ORDERS MANAGER
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Latest Web & WhatsApp Orders / آرڈرز کی تفصیل",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        // Auto trigger simulated order
                                        if (products.isEmpty()) {
                                            Toast.makeText(context, "Please add products in Inventory first!", Toast.LENGTH_LONG).show()
                                        } else {
                                            val randomProduct = products.random()
                                            val names = listOf("Hamza Iqbal", "Ameer Moavia", "Sadia Bibi", "Bilal Sindhu", "Kashif Jamil")
                                            val cities = listOf("Lahore", "Faisalabad", "Multan", "Gujranwala", "Rawalpindi")
                                            val randomName = names.random()
                                            val randomCity = cities.random()
                                            val randomPhone = "03${(100000000..999999999).random()}"
                                            val newOrder = OnlineOrder(
                                                id = "#${(1093..2100).random()}",
                                                customerName = "$randomName ($randomCity)",
                                                phone = randomPhone,
                                                timestamp = System.currentTimeMillis(),
                                                status = "Pending ⏳",
                                                items = listOf(OrderItem(randomProduct.name, (1..2).random(), randomProduct.sellingPrice)),
                                                total = randomProduct.sellingPrice * (1..2).random(),
                                                orderType = "Web Store 💻"
                                            )
                                            onlineOrders.add(0, newOrder)
                                            saveOrders()
                                            Toast.makeText(context, "New simulated Online Order received from $randomName!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Default.Bolt, null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Simulate Order ⚡", fontSize = 10.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Manual ADD Web Order Button
                            OutlinedButton(
                                onClick = { showAddManualOrderDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Record Offline / Social Media Order (نیا آرڈر درج کریں)", fontSize = 12.sp)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (onlineOrders.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No web orders yet.\nClick \"Simulate Order\" or share catalog link!",
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(onlineOrders, key = { it.id }) { order ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(
                                                            text = order.customerName,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = "Phone: ${order.phone} | ${order.orderType}",
                                                            fontSize = 10.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }

                                                    // Status badge with click rotation
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(
                                                                when {
                                                                    order.status.contains("Pending") -> Color(0xFFFFF3E0)
                                                                    order.status.contains("Packing") -> Color(0xFFE8F0FE)
                                                                    else -> Color(0xFFE8F5E9)
                                                                }
                                                            )
                                                            .clickable {
                                                                val currentStatus = order.status
                                                                val newStatus = when {
                                                                    currentStatus.contains("Pending") -> "Packing 📦"
                                                                    currentStatus.contains("Packing") -> "Delivered ✅"
                                                                    else -> "Pending ⏳"
                                                                }
                                                                val index = onlineOrders.indexOfFirst { it.id == order.id }
                                                                if (index != -1) {
                                                                    onlineOrders[index] = order.copy(status = newStatus)
                                                                    saveOrders()
                                                                }
                                                            }
                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(
                                                            text = order.status,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = when {
                                                                order.status.contains("Pending") -> Color(0xFFE65100)
                                                                order.status.contains("Packing") -> Color(0xFF1967D2)
                                                                else -> Color(0xFF1B5E20)
                                                            }
                                                        )
                                                    }
                                                }

                                                Divider(
                                                    modifier = Modifier.padding(vertical = 8.dp),
                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                                )

                                                // Ordered Items list
                                                order.items.forEach { item ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = "• ${item.productName} (x${item.qty})",
                                                            fontSize = 12.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            text = "Rs. ${String.format("%,.0f", item.price * item.qty)}",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                }

                                                Divider(
                                                    modifier = Modifier.padding(vertical = 8.dp),
                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                                )

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val formattedDate = remember(order.timestamp) {
                                                        SimpleDateFormat("EEE, hh:mm a", Locale.getDefault()).format(Date(order.timestamp))
                                                    }
                                                    Text(
                                                        text = "Ordered on: $formattedDate",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = "Total Value: Rs. ${String.format("%,.0f", order.total)}",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> { // TAB 1: DIGITAL CATALOG SHOWCASE
                        Column(modifier = Modifier.fillMaxSize()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Your Public Store Link / آن لائن دکان لنک:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val publicLink = "https://dukan.online/${storeName.lowercase().replace(" ", "-")}"
                                        Text(
                                            text = publicLink,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Row {
                                            IconButton(onClick = {
                                                clipboardManager.setText(AnnotatedString(publicLink))
                                                Toast.makeText(context, "Store Link copied to clipboard!", Toast.LENGTH_SHORT).show()
                                            }) {
                                                Icon(Icons.Default.ContentCopy, "Copy link", modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(onClick = {
                                                Toast.makeText(context, "WhatsApp checkout catalog shared!", Toast.LENGTH_SHORT).show()
                                            }) {
                                                Icon(Icons.Default.Share, "Share WhatsApp catalog link", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Select Products to Publish Online / آن لائن مال منتخب کریں",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            if (products.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Default store. Add products in main Inventory tab to list them here.",
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(products, key = { it.id }) { product ->
                                        val isOnline = onlineProductIds.contains(product.id.toString())
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isOnline) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                                else MaterialTheme.colorScheme.surface
                                            ),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isOnline) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = product.name,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "Stock: ${product.stock} units | Rs. ${product.sellingPrice}",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Text(
                                                        text = if (isOnline) "On Store" else "Hidden",
                                                        fontSize = 10.sp,
                                                        color = if (isOnline) MaterialTheme.colorScheme.primary else Color.Gray,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Switch(
                                                        checked = isOnline,
                                                        onCheckedChange = { checked ->
                                                            val updated = onlineProductIds.toMutableSet()
                                                            if (checked) {
                                                                updated.add(product.id.toString())
                                                            } else {
                                                                updated.remove(product.id.toString())
                                                            }
                                                            onlineProductIds = updated
                                                            sharedPrefs.edit().putStringSet("online_product_ids", updated).apply()
                                                        },
                                                        modifier = Modifier.scale(0.85f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> { // TAB 2: STORE SETTINGS
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            OutlinedTextField(
                                value = storeName,
                                onValueChange = { storeName = it },
                                label = { Text("Online Store Name / آن لائن دکان کا نام") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = storeDesc,
                                onValueChange = { storeDesc = it },
                                label = { Text("Tagline or Description / دکان کا تعارف") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 2
                            )

                            OutlinedTextField(
                                value = whatsappNum,
                                onValueChange = { whatsappNum = it },
                                label = { Text("WhatsApp Checkout Number (گاہک کا رابطہ نمبر)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Store Online Visibility / دکان آن لائن کریں",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Toggle store link live status & customer shopping catalog.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isLive,
                                    onCheckedChange = { checked ->
                                        isLive = checked
                                        sharedPrefs.edit().putBoolean("store_is_live", checked).apply()
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    sharedPrefs.edit()
                                        .putString("store_name", storeName)
                                        .putString("store_desc", storeDesc)
                                        .putString("store_whatsapp", whatsappNum)
                                        .putBoolean("store_is_live", isLive)
                                        .apply()
                                    Toast.makeText(context, "Online Store settings saved & went live! 🟢", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save Settings & Go Live / آن لائن کریں")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddManualOrderDialog) {
        var custName by remember { mutableStateOf("") }
        var custPh by remember { mutableStateOf("") }
        var selectedItemIndex by remember { mutableStateOf(0) }
        var qtyInput by remember { mutableStateOf("1") }

        AlertDialog(
            onDismissRequest = { showAddManualOrderDialog = false },
            title = { Text("Manual Web Order / نیا آن لائن آرڈر") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = custName,
                        onValueChange = { custName = it },
                        label = { Text("Customer Name / گاہک کا نام") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = custPh,
                        onValueChange = { custPh = it },
                        label = { Text("WhatsApp Phone / موبائل نمبر") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (products.isNotEmpty()) {
                        Text("Select Product:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        var expandedProductsDropdown by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { expandedProductsDropdown = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(products[selectedItemIndex].name)
                            }
                            DropdownMenu(
                                expanded = expandedProductsDropdown,
                                onDismissRequest = { expandedProductsDropdown = false }
                            ) {
                                products.forEachIndexed { i, p ->
                                    DropdownMenuItem(
                                        text = { Text("${p.name} (Rs. ${p.sellingPrice})") },
                                        onClick = {
                                            selectedItemIndex = i
                                            expandedProductsDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = qtyInput,
                            onValueChange = { qtyInput = it },
                            label = { Text("Quantity") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("No products in inventory yet!", color = Color.Red)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qtyVal = qtyInput.toIntOrNull() ?: 1
                        if (custName.isNotEmpty() && products.isNotEmpty() && qtyVal > 0) {
                            val prod = products[selectedItemIndex]
                            val newOrder = OnlineOrder(
                                id = "#${(1093..2100).random()}",
                                customerName = custName,
                                phone = custPh,
                                timestamp = System.currentTimeMillis(),
                                status = "Pending ⏳",
                                items = listOf(OrderItem(prod.name, qtyVal, prod.sellingPrice)),
                                total = prod.sellingPrice * qtyVal,
                                orderType = "Manual Web Order 📝"
                            )
                            onlineOrders.add(0, newOrder)
                            saveOrders()
                            showAddManualOrderDialog = false
                            Toast.makeText(context, "Manual online order received successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Fill out all fields correctly!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Record Order / درج کریں")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddManualOrderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Data models for the simulated/manual Online Store
data class OnlineOrder(
    val id: String,
    val customerName: String,
    val phone: String,
    val timestamp: Long,
    val status: String,
    val items: List<OrderItem>,
    val total: Double,
    val orderType: String
)

data class OrderItem(
    val productName: String,
    val qty: Int,
    val price: Double
)
