package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.scale
import androidx.lifecycle.ViewModelProvider
import com.example.data.database.AppDatabase
import com.example.data.repository.DukanRepository
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.SalesScreen
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DukanViewModel
import com.example.ui.viewmodel.DukanViewModelFactory
import android.widget.Toast
import com.example.ui.components.SettingsBottomSheet

enum class DukanTab(val englishLabel: String, val urduLabel: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", "رپورٹ", Icons.Default.Dashboard),
    PRODUCTS("Inventory", "مال", Icons.Default.Inventory2),
    SALES("Sales", "بکری", Icons.Default.ReceiptLong),
    CUSTOMERS("Customers", "گاہک", Icons.Default.People)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Room Database, Repository, and ViewModel using standard Factories
        val database = AppDatabase.getDatabase(this)
        val repository = DukanRepository(database)
        val factory = DukanViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, factory)[DukanViewModel::class.java]

        // Load persisted Google sign-in session
        val sp = getSharedPreferences("dukan_auth_pref", MODE_PRIVATE)
        val savedEmail = sp.getString("user_email", null)
        val savedName = sp.getString("user_name", null)
        if (savedEmail != null && savedName != null) {
            viewModel.login(savedEmail, savedName)
        }

        setContent {
            val themeSp = remember { getSharedPreferences("dukan_theme_pref", MODE_PRIVATE) }
            var isDarkMode by remember { mutableStateOf(themeSp.getBoolean("is_dark_mode", false)) }
            var shopNameState by remember { mutableStateOf(themeSp.getString("shop_name", "SHAWAL DIGITAL DUKAN") ?: "SHAWAL DIGITAL DUKAN") }
            var ownerNameState by remember { mutableStateOf(themeSp.getString("owner_name", "SHAWAL") ?: "SHAWAL") }

            MyApplicationTheme(darkTheme = isDarkMode) {
                val isLoggedIn by viewModel.isLoggedIn.collectAsState()

                if (!isLoggedIn) {
                    LoginScreen(
                        viewModel = viewModel,
                        onLoginSuccess = { email, name ->
                            sp.edit()
                                .putString("user_email", email)
                                .putString("user_name", name)
                                .apply()
                        }
                    )
                } else {
                    var currentTab by remember { mutableStateOf(DukanTab.DASHBOARD) }
                    var showSettingsSheet by remember { mutableStateOf(false) }

                    val context = androidx.compose.ui.platform.LocalContext.current
                    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
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

                    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
                    ) { uri ->
                        if (uri != null) {
                            try {
                                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                    val jsonString = inputStream.bufferedReader().use { it.readText() }
                                    viewModel.importBackupJson(jsonString) { success, message ->
                                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                        if (success) {
                                            shopNameState = themeSp.getString("shop_name", "SHAWAL DIGITAL DUKAN") ?: "SHAWAL DIGITAL DUKAN"
                                            ownerNameState = themeSp.getString("owner_name", "SHAWAL") ?: "SHAWAL"
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error opening backup: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }

                    if (showSettingsSheet) {
                        SettingsBottomSheet(
                            onDismissRequest = { showSettingsSheet = false },
                            onSaveSettings = { name, owner, limit ->
                                themeSp.edit()
                                    .putString("shop_name", name)
                                    .putString("owner_name", owner)
                                    .apply()
                                shopNameState = name
                                ownerNameState = owner
                                
                                val alertPref = context.getSharedPreferences("dukan_inventory_alert_prefs", android.content.Context.MODE_PRIVATE)
                                alertPref.edit().putInt("low_stock_threshold", limit).apply()
                            },
                            onExportBackup = {
                                exportLauncher.launch("dukan_inventory_backup.json")
                            },
                            onImportBackup = {
                                importLauncher.launch(arrayOf("application/json"))
                            }
                        )
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp)
                                    .padding(top = 34.dp, bottom = 4.dp),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = shopNameState,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = "ہمارا کاروبار، ہماری دکان • " + when(currentTab) {
                                                DukanTab.DASHBOARD -> "رپورٹ"
                                                DukanTab.PRODUCTS -> "مال"
                                                DukanTab.SALES -> "بکری"
                                                DukanTab.CUSTOMERS -> "گاہک"
                                            },
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }

                                    // Settings Cog & Switch Theme Toggle
                                    Row(
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = { showSettingsSheet = true },
                                            modifier = Modifier.testTag("settings_cog_icon_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Settings,
                                                contentDescription = "Settings Option",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                isDarkMode = !isDarkMode
                                                themeSp.edit().putBoolean("is_dark_mode", isDarkMode).apply()
                                            },
                                            modifier = Modifier.testTag("theme_toggle_icon_btn")
                                        ) {
                                            Icon(
                                                imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                                contentDescription = "Theme Toggle",
                                                tint = if (isDarkMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Switch(
                                            checked = isDarkMode,
                                            onCheckedChange = { checked ->
                                                isDarkMode = checked
                                                themeSp.edit().putBoolean("is_dark_mode", checked).apply()
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = MaterialTheme.colorScheme.tertiary,
                                                checkedTrackColor = MaterialTheme.colorScheme.tertiaryContainer
                                            ),
                                            modifier = Modifier.scale(0.85f).testTag("theme_toggle_switch")
                                        )
                                    }
                                }
                            }
                        },
                        bottomBar = {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(24.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            ) {
                                NavigationBar(
                                    tonalElevation = 0.dp,
                                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .windowInsetsPadding(WindowInsets.navigationBars)
                                        .testTag("bottom_nav_bar")
                                ) {
                                    DukanTab.entries.forEach { tab ->
                                        NavigationBarItem(
                                            selected = currentTab == tab,
                                            onClick = { currentTab = tab },
                                            icon = {
                                                Icon(
                                                    imageVector = tab.icon,
                                                    contentDescription = tab.englishLabel,
                                                    tint = if (currentTab == tab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            },
                                            label = {
                                                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = tab.englishLabel,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (currentTab == tab) FontWeight.ExtraBold else FontWeight.Medium,
                                                        color = if (currentTab == tab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = tab.urduLabel,
                                                        fontSize = 9.sp,
                                                        color = if (currentTab == tab) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                    )
                                                }
                                            },
                                            alwaysShowLabel = true,
                                            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            DukanTab.DASHBOARD -> DashboardScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                            DukanTab.PRODUCTS -> ProductsScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                            DukanTab.SALES -> SalesScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                            DukanTab.CUSTOMERS -> CustomersScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
                }
            }
        }
    }
}
