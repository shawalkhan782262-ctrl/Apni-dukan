package com.example.ui.components

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerDialog(
    availableProducts: List<Product>,
    onDismiss: () -> Unit,
    onProductScanned: (Product) -> Unit
) {
    val context = LocalContext.current
    var isFlashOn by remember { mutableStateOf(false) }
    var manualCode by remember { mutableStateOf("") }
    var scanError by remember { mutableStateOf("") }

    // Audio beep trigger
    val playBeep = {
        try {
            val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            tg.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Laser scanning animation
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_pos"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Product QR / Barcode Scanner",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("scanner_close")) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Outer scanner container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.9f))
                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Custom Camera scanning guide brackets
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .border(1.5.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    ) {
                        // Corner glowing lines
                        Box(modifier = Modifier.size(16.dp).align(Alignment.TopStart).border(2.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 8.dp)))
                        Box(modifier = Modifier.size(16.dp).align(Alignment.TopEnd).border(2.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(topEnd = 8.dp)))
                        Box(modifier = Modifier.size(16.dp).align(Alignment.BottomStart).border(2.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(bottomStart = 8.dp)))
                        Box(modifier = Modifier.size(16.dp).align(Alignment.BottomEnd).border(2.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(bottomEnd = 8.dp)))

                        // Laser line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .align(Alignment.TopCenter)
                                .offset(y = (130.dp * laserPosition))
                                .background(Color.Red)
                        )
                    }

                    // Simulated live status text overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = Color.Green,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                "LIVE FINDER ACTIVE",
                                color = Color.Green,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Flash Toggle Controls
                    IconButton(
                        onClick = { isFlashOn = !isFlashOn },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flash Toggle",
                            tint = Color.White
                        )
                    }
                }

                // Emulator / Demo fallback scan triggers as prominent "Simulation Mode"
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Simulator / Manual Demo Scan",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "Tap on any product below to simulate scanning its QR coupon or barcode instantly:",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (availableProducts.isEmpty()) {
                            Text(
                                "No inventory items to scan. Please add items to products inventory first.",
                                fontSize = 10.sp,
                                color = Color.Red,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 100.dp)
                            ) {
                                items(availableProducts) { product ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                playBeep()
                                                onProductScanned(product)
                                                onDismiss()
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            Column {
                                                Text(product.name, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                                Text("Rs. ${product.sellingPrice}", fontSize = 9.sp, color = Color.Gray)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Keyboard manual code lookup as a secondary backup option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = manualCode,
                        onValueChange = {
                            manualCode = it
                            scanError = ""
                        },
                        placeholder = { Text("Code lookup manually (id/name)") },
                        leadingIcon = { Icon(Icons.Default.Keyboard, contentDescription = null) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("scanner_manual_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall
                    )

                    Button(
                        onClick = {
                            if (manualCode.isNotEmpty()) {
                                // Find product by product code, name, or id matches
                                val match = availableProducts.find {
                                    it.id.toString() == manualCode ||
                                    it.name.contains(manualCode, ignoreCase = true)
                                }
                                if (match != null) {
                                    playBeep()
                                    onProductScanned(match)
                                    onDismiss()
                                } else {
                                    scanError = "Product code '$manualCode' not found in inventory."
                                }
                            }
                        },
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Lookup", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (scanError.isNotEmpty()) {
                    Text(
                        text = scanError,
                        color = Color.Red,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {},
        shape = RoundedCornerShape(24.dp)
    )
}
