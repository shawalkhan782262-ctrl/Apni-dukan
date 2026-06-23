package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.Customer
import com.example.ui.viewmodel.DukanViewModel
import com.example.ui.components.DirectPayQrBottomSheet

@Composable
fun CustomersScreen(
    viewModel: DukanViewModel,
    modifier: Modifier = Modifier
) {
    val customers by viewModel.customers.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    
    // Quick Credit & Debit Adjustment State
    var selectedCustomerForAdjustment by remember { mutableStateOf<Customer?>(null) }
    var isAddingCreditState by remember { mutableStateOf(true) }
    
    var selectedCustomerForEdit by remember { mutableStateOf<Customer?>(null) }
    var showQrForCustomer by remember { mutableStateOf<Customer?>(null) }

    val filteredCustomers = remember(customers, searchQuery) {
        customers.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val netTotalCredit = customers.sumOf { it.balance }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Net outstanding credits banner
            if (customers.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Kul Udhaar (Combined Outstanding Credit)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text("Rs. ${String.format("%,.0f", netTotalCredit)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(32.dp))
                    }
                }
            }

            // Search Bar & Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Grahak ka Naam dhoundain... (مثال: علی)") },
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
                        .testTag("customer_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Customers List
            if (filteredCustomers.isEmpty()) {
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
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = if (searchQuery.isEmpty()) "Koi Grahak nahi hai" else "Maching client nahi mila",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (searchQuery.isEmpty()) "Grahak add karne k liye niche diye button '+' par click karain, aur unka udhaar khata manage karay." else "Grahak ka naam correct type karain.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredCustomers, key = { it.id }) { customer ->
                        CustomerItemCard(
                            customer = customer,
                            onAdjustCreditClick = {
                                isAddingCreditState = true
                                selectedCustomerForAdjustment = customer
                            },
                            onAdjustDebitClick = {
                                isAddingCreditState = false
                                selectedCustomerForAdjustment = customer
                            },
                            onShowQrClick = {
                                showQrForCustomer = customer
                            },
                            onLongPressClick = { selectedCustomerForEdit = customer }
                        )
                    }
                }
            }
        }

        // Floating Action Button to Add Customer
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_customer_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = "Grahak shamil karay")
        }
    }

    // Add Customer Dialog
    if (showAddDialog) {
        CustomerDialog(
            title = "Naya Grahak Shamil Karain",
            onDismiss = { showAddDialog = false },
            onSave = { name, phone, balance, rating ->
                viewModel.addCustomer(name, phone, balance, rating)
                showAddDialog = false
            }
        )
    }

    // Edit/Manage Customer Profile Dialog
    selectedCustomerForEdit?.let { customer ->
        CustomerDialog(
            title = "Khata Tabdeel / Edit Profile",
            customer = customer,
            onDismiss = { selectedCustomerForEdit = null },
            onSave = { name, phone, balance, rating ->
                viewModel.updateCustomer(
                    customer.copy(
                        name = name,
                        phone = phone,
                        balance = balance,
                        rating = rating
                    )
                )
                selectedCustomerForEdit = null
            },
            onDelete = {
                viewModel.deleteCustomer(customer)
                selectedCustomerForEdit = null
            }
        )
    }

    // Comprehensive Credit/Debit Balance Adjustment Dialog
    selectedCustomerForAdjustment?.let { customer ->
        CreditDebitAdjustmentDialog(
            customer = customer,
            isAddingCredit = isAddingCreditState,
            onDismiss = { selectedCustomerForAdjustment = null },
            onAdjust = { amount ->
                val newBalance = if (isAddingCreditState) {
                    customer.balance + amount
                } else {
                    customer.balance - amount
                }
                viewModel.updateCustomer(customer.copy(balance = newBalance))
                selectedCustomerForAdjustment = null
            },
            onShowQr = {
                showQrForCustomer = customer
            }
        )
    }

    // Direct mobile Pay QR bottom sheet
    showQrForCustomer?.let { customer ->
        DirectPayQrBottomSheet(
            customer = customer,
            defaultAmount = customer.balance,
            onDismissRequest = { showQrForCustomer = null }
        )
    }
}

@Composable
fun CustomerItemCard(
    customer: Customer,
    onAdjustCreditClick: () -> Unit,
    onAdjustDebitClick: () -> Unit,
    onShowQrClick: () -> Unit,
    onLongPressClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onLongPressClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Customer avatar background indicator
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (customer.phone.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                            Text(
                                text = customer.phone,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    // Rating Star Indicator Row on card
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        (1..5).forEach { index ->
                            Icon(
                                imageVector = if (index <= customer.rating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = if (index <= customer.rating) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(${customer.rating}/5)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Balance Details Area
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val hasUdhaar = customer.balance > 0.0
                    if (hasUdhaar) {
                        Text(
                            text = "Udhaar (Due)",
                            fontSize = 11.sp,
                            color = Color(0xFFC62828),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Rs. ${String.format("%,.0f", customer.balance)}",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFC62828),
                            fontSize = 16.sp
                        )
                    } else if (customer.balance < 0.0) {
                        Text(
                            text = "Advance",
                            fontSize = 11.sp,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Rs. ${String.format("%,.0f", -customer.balance)}",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF2E7D32),
                            fontSize = 16.sp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                            Text(
                                text = "Saaf / Clear",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Quick Adjustment Action Buttons row on each card item
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Udhaar Diya Action (Adds Debt)
                Button(
                    onClick = onAdjustCreditClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("btn_add_credit_${customer.id}")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Udhaar Diya", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }

                // Udhaar Mila Action (Repays Debt)
                Button(
                    onClick = onAdjustDebitClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("btn_add_debit_${customer.id}")
                ) {
                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Udhaar Mila", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }

                // Show QR Pay Badge (Scan & Pay Direct)
                IconButton(
                    onClick = onShowQrClick,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(10.dp))
                        .size(36.dp)
                        .testTag("btn_show_qr_${customer.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Grahak Qr scanner direct pay",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CreditDebitAdjustmentDialog(
    customer: Customer,
    isAddingCredit: Boolean,
    onDismiss: () -> Unit,
    onAdjust: (Double) -> Unit,
    onShowQr: () -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }
    
    val presets = listOf(50, 100, 500, 1000)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isAddingCredit) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    contentDescription = null,
                    tint = if (isAddingCredit) Color(0xFFE53935) else Color(0xFF2E7D32)
                )
                Text(
                    text = if (isAddingCredit) "Naya Udhaar Dia (+)" else "Wapsi Udhaar Mila (-)",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isAddingCredit) {
                        "Grahak ${customer.name} ko udhaar maal diya hai. Record update karay:"
                    } else {
                        "Grahak ${customer.name} se udhaar raqam wapsi mili hai. Entry update karay:"
                    },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { preset ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                .clickable { amountText = preset.toString() }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+$preset", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Raqam (Enter Amount in Rs.)") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("adjust_amount_field"),
                    singleLine = true
                )

                if (hasError) {
                    Text(
                        text = "Meherbani kar k durust raqam add karain.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!isAddingCredit && customer.balance > 0.0) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    
                    Button(
                        onClick = {
                            onShowQr()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Show Direct Pay QR Code", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt != null && amt > 0.0) {
                        onAdjust(amt)
                    } else {
                        hasError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAddingCredit) Color(0xFFE53935) else Color(0xFF2E7D32)
                )
            ) {
                Text("Confirm", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
}

@Composable
fun CustomerDialog(
    title: String,
    customer: Customer? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, Double, Int) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(customer?.name ?: "") }
    var phone by remember { mutableStateOf(customer?.phone ?: "") }
    var balance by remember { mutableStateOf(customer?.balance?.toString() ?: "") }
    var rating by remember { mutableStateOf(customer?.rating ?: 5) }

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
                    label = { Text("Grahak ka Naam (Customer Name)") },
                    modifier = Modifier.fillMaxWidth().testTag("customer_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile Number (Phone)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().testTag("customer_phone_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = balance,
                    onValueChange = { balance = it },
                    label = { Text("Sabqa Udhaar / Previous Debt (Balance)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("0.0") },
                    modifier = Modifier.fillMaxWidth().testTag("customer_balance_input"),
                    singleLine = true
                )

                // Interactive Rating Selector Row
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Grahak Trust Score (Credit Rating):",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        (1..5).forEach { index ->
                            Icon(
                                imageVector = if (index <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "$index star ranking",
                                tint = if (index <= rating) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier
                                    .size(28.dp)
                                    .clickable { rating = index }
                                    .testTag("customer_star_select_$index")
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (rating) {
                                1 -> "Very Risky ⚠️"
                                2 -> "Slow Pay ⏳"
                                3 -> "Normal 👍"
                                4 -> "Good Trust ⭐"
                                5 -> "Super Trust 💎"
                                else -> "Normal"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (rating) {
                                1, 2 -> Color(0xFFD32F2F)
                                3 -> Color(0xFFE65100)
                                else -> Color(0xFF388E3C)
                            }
                        )
                    }
                }

                if (hasError) {
                    Text(
                        text = "Meherbani kar k Grahak ka naam durust likhain.",
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
                    val bal = balance.toDoubleOrNull() ?: 0.0
                    if (name.isNotEmpty()) {
                        onSave(name, phone, bal, rating)
                    } else {
                        hasError = true
                    }
                },
                modifier = Modifier.testTag("customer_dialog_save_btn")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.testTag("customer_dialog_dismiss_btn")) {
                    Text("Dismiss")
                }
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("customer_dialog_delete_btn")
                    ) {
                        Text("Delete")
                    }
                }
            }
        }
    )
}

@Composable
fun RepaymentDialog(
    customer: Customer,
    onDismiss: () -> Unit,
    onRepay: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wapsi Jama / Cash Return", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Grahak ${customer.name} owes Rs. ${customer.balance}. Enter how much they are returning now:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Wapsi Raqam (Cash Paid Back)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (hasError) {
                    Text(
                        text = "Raqam sahi se enter karain.",
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
                    val repayAmt = amountText.toDoubleOrNull()
                    if (repayAmt != null && repayAmt > 0 && repayAmt <= customer.balance) {
                        onRepay(repayAmt)
                    } else {
                        hasError = true
                    }
                }
            ) {
                Text("Repay")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
}
