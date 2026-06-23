package com.example.ui.screens

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import java.util.UUID
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.animation.*
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ProfitLossChart
import com.example.ui.components.ThirtyDaysSalesChart
import com.example.ui.components.DirectPayQrBottomSheet
import com.example.ui.components.RestockLocatorBottomSheet
import com.example.ui.components.OnlineStoreBottomSheet
import com.example.ui.viewmodel.DukanViewModel
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.R
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast

data class DukanReview(
    val id: String = UUID.randomUUID().toString(),
    val customerName: String,
    val rating: Int,
    val feedback: String,
    val timestamp: Long = System.currentTimeMillis()
)

fun saveDukanReviews(context: Context, reviews: List<DukanReview>) {
    val sp = context.getSharedPreferences("dukan_reviews_pref", Context.MODE_PRIVATE)
    val serialized = reviews.joinToString("||SRV||") { r ->
        "${r.id}**REV**${r.customerName}**REV**${r.rating}**REV**${r.feedback}**REV**${r.timestamp}"
    }
    sp.edit().putString("reviews_list", serialized).apply()
}

fun loadDukanReviews(context: Context): List<DukanReview> {
    val sp = context.getSharedPreferences("dukan_reviews_pref", Context.MODE_PRIVATE)
    val data = sp.getString("reviews_list", "") ?: ""
    if (data.isEmpty()) {
        return listOf(
            DukanReview(customerName = "Ali Raza", rating = 5, feedback = "Gaming zone aur maal bohat kamal ka hai! Munasib daam hai."),
            DukanReview(customerName = "Zain Gaming", rating = 5, feedback = "Shawal FF is the best creator, store design features are amazing."),
            DukanReview(customerName = "Hamza Butt", rating = 4, feedback = "Bargain system bohat acha laga. Customer rating trust build krti hai.")
        )
    }
    return try {
        data.split("||SRV||").map { rStr ->
            val parts = rStr.split("**REV**")
            DukanReview(
                id = parts[0],
                customerName = parts[1],
                rating = parts[2].toInt(),
                feedback = parts[3],
                timestamp = parts[4].toLong()
            )
        }
    } catch (e: Exception) {
        listOf(
            DukanReview(customerName = "Ali Raza", rating = 5, feedback = "Gaming zone aur maal bohat kamal ka hai! Munasib daam hai."),
            DukanReview(customerName = "Zain Gaming", rating = 5, feedback = "Shawal FF is the best creator, store design features are amazing."),
            DukanReview(customerName = "Hamza Butt", rating = 4, feedback = "Bargain system bohat acha laga. Customer rating trust build krti hai.")
        )
    }
}

@Composable
fun DashboardScreen(
    viewModel: DukanViewModel,
    modifier: Modifier = Modifier
) {
    val products by viewModel.products.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val todayReport by viewModel.todayReport.collectAsState()
    val chartData by viewModel.profitChartData.collectAsState()
    val thirtyDaysSalesData by viewModel.thirtyDaysSalesChartData.collectAsState()
    val salesList by viewModel.sales.collectAsState()

    var showCalculator by remember { mutableStateOf(false) }
    var showOnlinePrice by remember { mutableStateOf(false) }
    var showPayQrCreator by remember { mutableStateOf(false) }
    var showRestockLocator by remember { mutableStateOf(false) }
    var showOnlineStore by remember { mutableStateOf(false) }
    var dashboardTabState by remember { mutableStateOf(0) }
    var saleIdSearchQuery by remember { mutableStateOf("") }

    val context = LocalContext.current
    val themeSp = remember { context.getSharedPreferences("dukan_theme_pref", android.content.Context.MODE_PRIVATE) }
    val dukanOwner = themeSp.getString("owner_name", "SHAWAL") ?: "SHAWAL"
    val dukanName = themeSp.getString("shop_name", "APNI DUKAN") ?: "APNI DUKAN"

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            val csvHeader = "Sale ID,Date,Customer Name,Sales Price (Rs),Kharid Cost (Rs),Profit (Rs)\n"
            val csvRows = salesList.map { sale ->
                val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(sale.timestamp))
                val profit = sale.totalPrice - sale.totalCost
                "${sale.id},${dateStr},\"${sale.customerName ?: "Walk-in Customer"}\",${sale.totalPrice},${sale.totalCost},${profit}"
            }.joinToString("\n")
            val csvContent = csvHeader + csvRows
            try {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(csvContent.toByteArray())
                }
                Toast.makeText(context, "Mubarak! Daily Sales CSV exported successfully!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val reviewsList = remember { mutableStateListOf<DukanReview>().apply {
        addAll(loadDukanReviews(context))
    } }
    var showAddReviewDialog by remember { mutableStateOf(false) }

    val outOfStockCount = products.count { it.stock == 0 }
    val lowStockCount = products.count { it.stock in 1..5 }
    val totalUdhaar = customers.sumOf { it.balance }

    val (todaySales, todayCost, todayProfit) = todayReport

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackupJson { jsonString ->
                if (jsonString.isNotEmpty()) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(jsonString.toByteArray())
                        }
                        Toast.makeText(context, "Backup JSON file created successfully!", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error saving backup: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "Failed to create backup data.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val jsonString = inputStream.bufferedReader().use { it.readText() }
                    viewModel.importBackupJson(jsonString) { success, message ->
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error opening backup: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val shareText = "Apni Dukan (اپنی دکان): Mobiles, Karyana, Electronics! Swagat hai apka hamari modern dukan par! Dynamic services curated by Owner Shawal."
    val sendIntent = remember {
        android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
    }
    val shareIntent = remember { android.content.Intent.createChooser(sendIntent, "Share Apni Dukan") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingShareButton(
                onClick = { context.startActivity(shareIntent) },
                modifier = Modifier.testTag("fixed_share_dukan_fab")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Multi-Category Commercial Brand Banner (Mobiles, Karyana, Electronics) owned by SHAWAL
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_shawal_multicategory_banner_1782045448183),
                            contentDescription = "SHAWAL Store Banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Semi-transparent overlay with elegant text
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.8f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.padding(bottom = 2.dp)
                            ) {
                                Text(
                                    text = " ★ OWNER: ${dukanOwner.uppercase()} ★ ",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = dukanName.uppercase(),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = Color.White
                            )
                            Text(
                                text = "Mobiles • Karyana • Electronics (موبائل، کریانہ، الیکٹرانکس)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            // Segment tab selector bar
            item {
                val tabTitles = listOf("Reports & Graphs", "Sales Ledger / بکری", "Ratings & Reviews")
                val tabIcons = listOf(Icons.Default.QueryStats, Icons.Default.ReceiptLong, Icons.Default.Star)
                
                TabRow(
                    selectedTabIndex = dashboardTabState,
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = dashboardTabState == index,
                            onClick = { dashboardTabState = index },
                            text = { Text(title, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1) },
                            icon = { Icon(tabIcons[index], contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("dashboard_seg_tab_$index")
                        )
                    }
                }
            }

        // Today's Main Profit & Loss Overview Row
        if (dashboardTabState == 0) {
            item {
                Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Today Sales
                KpiCard(
                    title = "Bikri (Today Sales)",
                    value = "Rs. ${String.format("%,.0f", todaySales)}",
                    icon = Icons.Default.Payments,
                    iconColor = MaterialTheme.colorScheme.primary,
                    bgColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                    modifier = Modifier.weight(1f)
                )

                // Today Profit
                val profitColor = if (todayProfit >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                KpiCard(
                    title = "Munafa (Net Profit)",
                    value = "Rs. ${String.format("%,.0f", todayProfit)}",
                    icon = if (todayProfit >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    iconColor = profitColor,
                    bgColor = if (todayProfit >= 0) Color(0xE8E8F5E9) else Color(0xFFFFEBEE),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Second Row: Udhaar and Inventory Health
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Outstanding Credits (Udhaar)
                KpiCard(
                    title = "Kul Udhaar (Total Credit)",
                    value = "Rs. ${String.format("%,.0f", totalUdhaar)}",
                    icon = Icons.Default.AccountBalanceWallet,
                    iconColor = if (totalUdhaar > 0) Color(0xFFD84315) else MaterialTheme.colorScheme.secondary,
                    bgColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                    modifier = Modifier.weight(1f)
                )

                // Stock Check
                val hasStockIssues = outOfStockCount > 0 || lowStockCount > 0
                KpiCard(
                    title = "Stock Alert",
                    value = if (hasStockIssues) "Out: $outOfStockCount | Low: $lowStockCount" else "Maal Bohat Hai",
                    icon = Icons.Default.Inventory,
                    iconColor = if (outOfStockCount > 0) Color(0xFFC62828) else if (lowStockCount > 0) Color(0xFFD84315) else Color(0xFF2E7D32),
                    bgColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Karobar Helper Tools Card Row (Compact side-by-side grid)
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Dukan Tools & Backup (کاروباری اوزار)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )

                // Row 1: Calculator & Online Market Rates
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Calculator Card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showCalculator = true }
                            .testTag("dashboard_tool_calculator")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Calculator",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "حساب کتاب",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    // Online Market Rates Card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showOnlinePrice = true }
                            .testTag("dashboard_tool_online_price")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.tertiary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Online Rates",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = "آن لائن قیمتیں",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                // Row 2: QR Payment & Supplier Locator (Compact Side-by-Side!)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // QR Pay Creator Card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showPayQrCreator = true }
                            .testTag("dashboard_tool_qr_pay")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "QR Creator",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "کیو آر کوڈ",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    // Wholesale Supplier Locator Card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showRestockLocator = true }
                            .testTag("dashboard_tool_restock_locator")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalShipping,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Suppliers Map",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "سپلائر تلاش کریں",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                // Row 3: JSON Backup & JSON Restore (Direct integration!)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Export Backup JSON
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { exportLauncher.launch("dukan_inventory_backup.json") }
                            .testTag("dashboard_tool_backup")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Backup,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Backup JSON",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "فائل بیک اپ",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Import Restore JSON
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { importLauncher.launch(arrayOf("application/json")) }
                            .testTag("dashboard_tool_restore")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Restore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Restore JSON",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "بیک اپ بحال",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Row 4: Online Sales Store & Web Orders
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showOnlineStore = true }
                        .testTag("dashboard_tool_online_store")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Online Store & Orders Manager",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF2E7D32))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "LIVE 🟢",
                                            color = Color.White,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                         )
                                    }
                                }
                                Text(
                                    text = "آن لائن سیل سلیز دکان سسٹم - Setup digital catalog, process WhatsApp checkouts & simulated orders",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                    lineHeight = 12.sp
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // 2. Sales Ledger / Bikri (بکری کا کھاتا) Tab
    if (dashboardTabState == 1) {
        // Title and CSV Export action card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp).testTag("sales_ledger_main_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Bikri (Sales Ledger & Accounts)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Daily transactions record & accounting export",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val totalRevenueSum = salesList.sumOf { it.totalPrice }
                            val totalProfitSum = salesList.sumOf { it.totalPrice - it.totalCost }
                            Text("Kul Sales: Rs. ${String.format("%,.0f", totalRevenueSum)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Kul Munafa: Rs. ${String.format("%,.0f", totalProfitSum)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
                        
                        // CSV Exporter Button trigger
                        Button(
                            onClick = {
                                val fileName = "SHAWAL_SALES_LEDGER_${java.text.SimpleDateFormat("yyyy_MM_dd", java.util.Locale.getDefault()).format(java.util.Date())}.csv"
                                csvLauncher.launch(fileName)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("export_csv_btn")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export CSV Ledger", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        
        // Search Input Row for Sales Ledger
        item {
            OutlinedTextField(
                value = saleIdSearchQuery,
                onValueChange = { saleIdSearchQuery = it },
                placeholder = { Text("Search sales by transaction ID or customer...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().testTag("sales_ledger_search_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }
        
        // Ledger items
        val filteredSales = salesList.filter { sale ->
            saleIdSearchQuery.isEmpty() || 
            sale.id.toString().contains(saleIdSearchQuery) || 
            (sale.customerName?.contains(saleIdSearchQuery, ignoreCase = true) ?: false)
        }.sortedByDescending { it.timestamp }
        
        if (filteredSales.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.HourglassEmpty, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Koi bikri record nahi mila (No Sales)", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            items(
                items = filteredSales,
                key = { it.id }
            ) { sale ->
                SaleLedgerItemRow(sale = sale, viewModel = viewModel)
            }
        }
    }

        // 4. Dukan Rating & Reviews System Card
        if (dashboardTabState == 2) {
            item {
                var expandedReviews by remember { mutableStateOf(false) }
                val avgNum = if (reviewsList.isEmpty()) 5.0 else reviewsList.sumOf { it.rating }.toDouble() / reviewsList.size
                val avgFormatted = String.format("%.1f", avgNum)

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Dukan Rating & Reviews",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Grahak ke tajarbaat aur reviews",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Add review button
                        Button(
                            onClick = { showAddReviewDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp).testTag("add_dukan_review_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Review", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    // Rating Summary Block
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = avgFormatted,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                (1..5).forEach { starIndex ->
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (starIndex <= avgNum.toInt()) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${reviewsList.size} Reviews",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Star ratings distribution bars
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            (5 downTo 1).forEach { starLevel ->
                                val count = reviewsList.count { it.rating == starLevel }
                                val proportion = if (reviewsList.isEmpty()) 0f else count.toFloat() / reviewsList.size
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "$starLevel",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(10.dp)
                                    )
                                    LinearProgressIndicator(
                                        progress = { proportion },
                                        color = Color(0xFFFFC107),
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(5.dp)
                                            .clip(CircleShape)
                                    )
                                    Text(
                                        text = "$count",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(14.dp),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    // Show list of reviews (First 2, expandable)
                    val displayedReviews = if (expandedReviews) reviewsList else reviewsList.take(2)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        displayedReviews.forEach { review ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = review.customerName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                                            (1..5).forEach { starIndex ->
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = if (starIndex <= review.rating) Color(0xFFFFC107) else Color.LightGray,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = review.feedback,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (reviewsList.size > 2) {
                            TextButton(
                                onClick = { expandedReviews = !expandedReviews },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text(
                                    text = if (expandedReviews) "Show Less" else "Show All (${reviewsList.size}) Reviews",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Dukan Actions / Hints
        if (dashboardTabState == 0 && products.isEmpty() && customers.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Tips",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "Shuruat Karain!",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Sab se pehle Inventory tab mein ja kar apni items (maal) add karain. Us k bad ap Sales shuru karskty hain.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }

    item {
        if (showCalculator) {
        DukanCalculatorBottomSheet(
            onDismissRequest = { showCalculator = false }
        )
    }

    if (showOnlinePrice) {
        OnlinePriceBottomSheet(
            onDismissRequest = { showOnlinePrice = false }
        )
    }

    if (showPayQrCreator) {
        DirectPayQrBottomSheet(
            onDismissRequest = { showPayQrCreator = false }
        )
    }

    if (showRestockLocator) {
        RestockLocatorBottomSheet(
            onDismissRequest = { showRestockLocator = false }
        )
    }

    if (showOnlineStore) {
        OnlineStoreBottomSheet(
            onDismissRequest = { showOnlineStore = false },
            viewModel = viewModel
        )
    }

    if (showAddReviewDialog) {
        var customerNameInput by remember { mutableStateOf("") }
        var reviewRating by remember { mutableStateOf(5) }
        var feedbackInput by remember { mutableStateOf("") }
        val templates = listOf(
            "Bohat kamal ki dukan hai! Rates bohat munasib hain.",
            "Shawal FF created custom feature, credit bookkeeping works perfectly.",
            "Udaahar manage krna asan hai, dynamic receipts system outstanding!",
            "Fast business checkout, clear transactions details."
        )

        AlertDialog(
            onDismissRequest = { showAddReviewDialog = false },
            title = { Text("Customer Feedback Register Karay", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = customerNameInput,
                        onValueChange = { customerNameInput = it },
                        label = { Text("Customer ka Naam") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("review_cust_name")
                    )

                    Column {
                        Text("Dukan Star Rating:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                            (1..5).forEach { starIndex ->
                                Icon(
                                    imageVector = if (starIndex <= reviewRating) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "$starIndex Stars",
                                    tint = if (starIndex <= reviewRating) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clickable { reviewRating = starIndex }
                                        .testTag("review_star_select_$starIndex")
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = feedbackInput,
                        onValueChange = { feedbackInput = it },
                        label = { Text("Review Comments (Urdu / English)") },
                        modifier = Modifier.fillMaxWidth().testTag("review_feedback_text")
                    )

                    Text("Pehle se tay shuda comments select karain:", fontSize = 11.sp, color = Color.Gray)
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(templates) { comment ->
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable { feedbackInput = comment }
                            ) {
                                Text(
                                    text = comment,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customerNameInput.isNotEmpty() && feedbackInput.isNotEmpty()) {
                            val newReview = DukanReview(
                                customerName = customerNameInput,
                                rating = reviewRating,
                                feedback = feedbackInput
                            )
                            reviewsList.add(0, newReview)
                            saveDukanReviews(context, reviewsList.toList())
                            showAddReviewDialog = false
                        }
                    },
                    modifier = Modifier.testTag("review_dialog_save_btn"),
                    enabled = customerNameInput.isNotEmpty() && feedbackInput.isNotEmpty()
                ) {
                    Text("Save Review")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddReviewDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
}
}
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, iconColor.copy(alpha = 0.18f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SaleLedgerItemRow(
    sale: com.example.data.database.Sale,
    viewModel: DukanViewModel
) {
    var isExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .testTag("sale_receipt_card_${sale.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "RECEIPT #${sale.id}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val timeStr = java.text.SimpleDateFormat("dd MMM hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(sale.timestamp))
                        Text(
                            text = timeStr,
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Rs. ${String.format("%,.0f", sale.totalPrice)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1B5E20)
                    )
                    val saleProfit = sale.totalPrice - sale.totalCost
                    Text(
                        text = "Munafa: Rs. ${String.format("%,.0f", saleProfit)}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (saleProfit >= 0) Color(0xFF2E7D32) else Color.Red
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Grahak (Customer): ${sale.customerName ?: "Walk-in Customer (نامعلوم گاہک)"}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            
            // Expanded breakdown view
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                val saleItems by viewModel.getSaleItems(sale.id).collectAsState(initial = emptyList())
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "SAMAN BREAKDOWN (اشیاء کی تفصیل):",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    if (saleItems.isEmpty()) {
                        Text("Breakdown lines loading...", fontSize = 11.sp, color = Color.Gray)
                    } else {
                        saleItems.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.productName} (x${item.quantity})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Rs. ${String.format("%,.0f", item.sellingPrice * item.quantity)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }
                        
                        Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Sales Price Net Total:", fontSize = 11.sp, color = Color.Gray)
                            Text("Rs. ${String.format("%,.0f", sale.totalPrice)}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Purchase Cost (Kharid):", fontSize = 11.sp, color = Color.Gray)
                            Text("Rs. ${String.format("%,.0f", sale.totalCost)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }

@Composable
fun FloatingShareButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        text = { Text("Share Dukan", fontWeight = FontWeight.Black, fontSize = 12.sp) },
        icon = { Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp)) },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    )
}
