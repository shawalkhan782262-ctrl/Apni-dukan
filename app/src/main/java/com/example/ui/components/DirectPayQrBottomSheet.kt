package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.Customer

enum class PaymentPlatform(val displayName: String, val urduName: String, val themeColor: Color) {
    EASYPAISA("EasyPaisa", "ایزی پیسہ", Color(0xFF388E3C)),
    JAZZCASH("JazzCash", "جاز کیش", Color(0xFFFFB300)),
    SADAPAY("SadaPay", "سدا پے", Color(0xFFFF5252)),
    NAYAPAY("NayaPay", "نیا پے", Color(0xFF0288D1)),
    BANK_TRANSFER("Bank Transfer", "بینک اکاؤنٹ", Color(0xFF6A1B9A))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectPayQrBottomSheet(
    customer: Customer? = null,
    defaultAmount: Double = 0.0,
    onDismissRequest: () -> Unit
) {
    val preferences = LocalContext.current.getSharedPreferences("dukan_payment_qr", android.content.Context.MODE_PRIVATE)
    
    var shopName by remember { 
        mutableStateOf(preferences.getString("shop_name", "Hamara Dukan") ?: "Hamara Dukan") 
    }
    var accountNo by remember { 
        mutableStateOf(preferences.getString("account_no", "") ?: "") 
    }
    var selectedPlatform by remember { 
        mutableStateOf(
            try {
                PaymentPlatform.valueOf(preferences.getString("platform", PaymentPlatform.EASYPAISA.name) ?: PaymentPlatform.EASYPAISA.name)
            } catch (e: Exception) {
                PaymentPlatform.EASYPAISA
            }
        ) 
    }
    
    var amountText by remember { 
        mutableStateOf(if (defaultAmount > 0) defaultAmount.toInt().toString() else "") 
    }
    var showBuilderMode by remember { mutableStateOf(accountNo.isEmpty()) }
    
    val paymentPayload = remember(selectedPlatform, accountNo, amountText, shopName) {
        val amtPart = if (amountText.isNotEmpty()) "&am=${amountText}" else ""
        if (selectedPlatform == PaymentPlatform.EASYPAISA) {
            "easypaisa://pay?receiver=${accountNo}&name=${shopName}${amtPart}"
        } else if (selectedPlatform == PaymentPlatform.JAZZCASH) {
            "jazzcash://pay?account=${accountNo}&name=${shopName}${amtPart}"
        } else {
            "payment://direct?platform=${selectedPlatform.name}&account=${accountNo}&name=${shopName}${amtPart}"
        }
    }

    val qrBitmap = remember(paymentPayload) {
        if (accountNo.isNotEmpty()) {
            QrCodeGenerator.generateQrCode(paymentPayload, 400)
        } else null
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pay Direct QR Creator",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "گاہک سے اسکین پی اور کیو آر ادائیگی",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { showBuilderMode = !showBuilderMode },
                        modifier = Modifier.background(
                            if (showBuilderMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Setup payment details",
                            tint = if (showBuilderMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            if (showBuilderMode) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Apni Payment Details Set Karain:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = shopName,
                            onValueChange = { shopName = it },
                            label = { Text("Dukan ya Mall ka Naam") },
                            placeholder = { Text("e.g. Madina Store") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Storefront, null) }
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Payment Platform Select Karain:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PaymentPlatform.entries.take(3).forEach { platform ->
                                    val isSelected = selectedPlatform == platform
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) platform.themeColor else platform.themeColor.copy(alpha = 0.08f))
                                            .clickable { selectedPlatform = platform }
                                            .padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = platform.displayName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else platform.themeColor
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PaymentPlatform.entries.takeLast(2).forEach { platform ->
                                    val isSelected = selectedPlatform == platform
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) platform.themeColor else platform.themeColor.copy(alpha = 0.08f))
                                            .clickable { selectedPlatform = platform }
                                            .padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = platform.displayName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else platform.themeColor
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = accountNo,
                            onValueChange = { accountNo = it },
                            label = { Text("Account No / Mobile No") },
                            placeholder = { Text("e.g. 03001234567") },
                            modifier = Modifier.fillMaxWidth().testTag("payment_account_no_field"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Icon(Icons.Default.CreditCard, null) }
                        )

                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            label = { Text("Payment Amount (Raqam) - Optional") },
                            placeholder = { Text("e.g. 1500") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Icon(Icons.Default.Paid, null) }
                        )

                        Button(
                            onClick = {
                                preferences.edit()
                                    .putString("shop_name", shopName)
                                    .putString("account_no", accountNo)
                                    .putString("platform", selectedPlatform.name)
                                    .apply()
                                showBuilderMode = false
                            },
                            modifier = Modifier.fillMaxWidth().testTag("save_payment_details_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save & Generate QR", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Text(
                    text = "Grahak se kahein k apnay mobile se scan karain:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (qrBitmap != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(4.dp, selectedPlatform.themeColor.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = shopName.uppercase(),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.Black
                                )
                                Text(
                                    text = "DIRECT QR PAYMENT",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    letterSpacing = 1.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(240.dp)
                                    .border(2.dp, Color.LightGray, RoundedCornerShape(16.dp))
                                    .background(Color.White)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = qrBitmap,
                                    contentDescription = "Payment QR Code Matrix",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(selectedPlatform.themeColor)
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${selectedPlatform.displayName} Pay",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                }
                                Text(
                                    text = "Account: $accountNo",
                                    fontSize = 12.sp,
                                    color = Color.DarkGray,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }

                            if (amountText.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .background(
                                            selectedPlatform.themeColor.copy(alpha = 0.1f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 20.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "Payable: ",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = "Rs. ${String.format("%,d", amountText.toIntOrNull() ?: 0)}",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = selectedPlatform.themeColor
                                    )
                                }
                            }
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = selectedPlatform.themeColor.copy(alpha = 0.08f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "ایزی پیسہ، جاز کیش یا کسی بھی بینک ایپ سے اسکین کریں!",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = selectedPlatform.themeColor,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Scan this QR code using EasyPaisa, JazzCash, or any banking app to complete the digital payment transfer instantly.",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showBuilderMode = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Details")
                        }
                        
                        Button(
                            onClick = onDismissRequest,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = selectedPlatform.themeColor)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Theek Hai")
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(80.dp)
                        )
                        Text(
                            text = "Payment Setup Required",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Dukan payment QR ready nahi hai. Setup screen par details darj kar k save karain.",
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = { showBuilderMode = true }) {
                            Text("Setup Now")
                        }
                    }
                }
            }
        }
    }
}
