package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.data.database.Product
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductQrCodeDialog(
    product: Product,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val qrSize = 380

    // Encode standard deep-link or raw product ID for scan lookup compatibility
    val qrPayload = product.id.toString()
    val qrBitmap = remember(product.id) {
        QrCodeGenerator.generateQrCode(qrPayload, qrSize)
    }

    var isPrinting by remember { mutableStateOf(false) }
    var printProgress by remember { mutableStateOf(0f) }

    // Printing simulation coroutine anim
    LaunchedEffect(isPrinting) {
        if (isPrinting) {
            printProgress = 0f
            while (printProgress < 1f) {
                kotlinx.coroutines.delay(100)
                printProgress += 0.08f
            }
            isPrinting = false
            Toast.makeText(context, "Mubarak! QR Label printed successfully / پرنٹ مکمل ہو گیا!", Toast.LENGTH_LONG).show()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .testTag("product_qr_dialog_container")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
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
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Print Product QR Tag",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("qr_dialog_dismiss")) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Standardized ISO-Label Printable Card layout
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val preferences = remember(context) { context.getSharedPreferences("dukan_theme_pref", android.content.Context.MODE_PRIVATE) }
                        val shopName = remember(preferences) { preferences.getString("shop_name", "SHAWAL DIGITAL DUKAN") ?: "SHAWAL DIGITAL DUKAN" }
                        // Brand Label
                        Text(
                            text = "★ $shopName ★",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.DarkGray,
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = "ہمارا کاروبار، ہماری دکان",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )

                        Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp)

                        // QR Image Container with nice clean offset border
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (qrBitmap != null) {
                                Image(
                                    bitmap = qrBitmap,
                                    contentDescription = "Generated Product QR Code",
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    "QR Code failed",
                                    fontSize = 10.sp,
                                    color = Color.Red,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Product Title & Metadata
                        Text(
                            text = product.name.uppercase(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text("ZAT / CLASS", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text(product.category, fontSize = 11.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("BECHNA (PRICE)", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text("Rs. ${String.format("%,.0f", product.sellingPrice)}", fontSize = 14.sp, color = Color(0xFF1B5E20), fontWeight = FontWeight.Black)
                            }
                        }

                        Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Item Code: #${product.id}",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "In Stock: ${product.stock}",
                                fontSize = 10.sp,
                                color = if (product.stock <= product.minStockThreshold) Color.Red else Color(0xFF1B5E20),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Printing progress anim
                AnimatedVisibility(visible = isPrinting) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Simulating Wireless Thermal Print... ${(printProgress * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        LinearProgressIndicator(
                            progress = printProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                }

                // Command Buttons Action Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Print trigger
                    Button(
                        onClick = { isPrinting = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_qr_print_tag"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        enabled = !isPrinting
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Print Label", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Share trigger
                    Button(
                        onClick = {
                            if (qrBitmap != null) {
                                shareQrCodeWithProduct(context, product, qrBitmap.asAndroidBitmap())
                            } else {
                                Toast.makeText(context, "Error sharing QR tag", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_qr_share_tag"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        enabled = !isPrinting
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Creates an elegant printable tag layout on a canvas and shares it as a PNG image dynamically.
 */
private fun shareQrCodeWithProduct(context: Context, product: Product, qrBitmap: Bitmap) {
    try {
        // Draw elegant composite product tag sticker
        val width = 450
        val height = 650
        val surface = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(surface)
        
        // Fill canvas white background
        canvas.drawColor(AndroidColor.WHITE)

        val paint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        // Draw border
        paint.color = AndroidColor.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawRect(8f, 8f, width.toFloat() - 8f, height.toFloat() - 8f, paint)

        val prefs = context.getSharedPreferences("dukan_theme_pref", Context.MODE_PRIVATE)
        val shopNameStr = prefs.getString("shop_name", "SHAWAL DIGITAL DUKAN") ?: "SHAWAL DIGITAL DUKAN"
        // Draw header branding
        paint.style = Paint.Style.FILL
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText("★ $shopNameStr ★", (width / 2).toFloat(), 45f, paint)

        paint.textSize = 14f
        paint.isFakeBoldText = false
        paint.color = AndroidColor.DKGRAY
        canvas.drawText("ہمارا کاروبار، ہماری دکان", (width / 2).toFloat(), 75f, paint)

        // Line
        paint.strokeWidth = 2f
        canvas.drawLine(15f, 95f, (width - 15).toFloat(), 95f, paint)

        // Draw QR
        val qrLeft = (width - 250) / 2f
        val qrTop = 115f
        val destRect = android.graphics.Rect(qrLeft.toInt(), qrTop.toInt(), (qrLeft + 250).toInt(), (qrTop + 250).toInt())
        canvas.drawBitmap(qrBitmap, null, destRect, paint)

        // Draw Details
        paint.color = AndroidColor.BLACK
        paint.textSize = 30f
        paint.isFakeBoldText = true
        canvas.drawText(product.name.uppercase(), (width / 2).toFloat(), 410f, paint)

        paint.textSize = 16f
        paint.isFakeBoldText = false
        paint.color = AndroidColor.GRAY
        canvas.drawText("Zat / Category: ${product.category}", (width / 2).toFloat(), 450f, paint)

        paint.color = AndroidColor.parseColor("#1B5E20") // Green price color
        paint.textSize = 26f
        paint.isFakeBoldText = true
        canvas.drawText("Rs. ${String.format("%,.0f", product.sellingPrice)}", (width / 2).toFloat(), 500f, paint)

        paint.color = AndroidColor.BLACK
        paint.strokeWidth = 2f
        canvas.drawLine(15f, 530f, (width - 15).toFloat(), 530f, paint)

        // Bottom specs
        paint.textSize = 15f
        paint.isFakeBoldText = true
        paint.color = AndroidColor.DKGRAY
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Item Code: #${product.id}", 30f, 575f, paint)

        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("In Stock: ${product.stock}", (width - 30).toFloat(), 575f, paint)

        paint.textSize = 12f
        paint.isFakeBoldText = false
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Scan QR code directly via Sales checkout scanner", (width / 2).toFloat(), 620f, paint)

        // Save bitmap to file system to share via intent provider
        val imagesFolder = File(context.cacheDir, "images")
        imagesFolder.mkdirs()
        val file = File(imagesFolder, "product_qr_${product.id}.png")
        FileOutputStream(file).use { stream ->
            surface.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }

        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "image/png"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share Product QR Tag Sticker")
        context.startActivity(chooser)
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Sharing failed: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
