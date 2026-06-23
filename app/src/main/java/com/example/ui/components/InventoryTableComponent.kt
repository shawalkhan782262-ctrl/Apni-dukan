package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.Product
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.SolidColor

enum class ProductSortField {
    NAME,
    STOCK,
    COST_PRICE,
    SELLING_PRICE,
    MARGIN
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InventoryTableComponent(
    products: List<Product>,
    onIncrementStock: (Product, Int) -> Unit,
    onEditProduct: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit,
    onGenerateQrCode: (Product) -> Unit,
    lowStockThreshold: Int = 5,
    modifier: Modifier = Modifier
) {
    var sortField by remember { mutableStateOf(ProductSortField.NAME) }
    var sortAscending by remember { mutableStateOf(true) }

    // Incremental Stock Fast-load Sheet / Alert Banner state
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    
    // Sort products
    val sortedProducts = remember(products, sortField, sortAscending) {
        val sortedList = when (sortField) {
            ProductSortField.NAME -> products.sortedBy { it.name.lowercase() }
            ProductSortField.STOCK -> products.sortedBy { it.stock }
            ProductSortField.COST_PRICE -> products.sortedBy { it.costPrice }
            ProductSortField.SELLING_PRICE -> products.sortedBy { it.sellingPrice }
            ProductSortField.MARGIN -> products.sortedBy { it.sellingPrice - it.costPrice }
        }
        if (sortAscending) sortedList else sortedList.reversed()
    }

    // Dynamic sort toggle helper
    val toggleSort = { field: ProductSortField ->
        if (sortField == field) {
            sortAscending = !sortAscending
        } else {
            sortField = field
            sortAscending = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Feedback alert bar
        AnimatedVisibility(
            visible = feedbackMessage != null,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            feedbackMessage?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            Text(msg, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                        IconButton(onClick = { feedbackMessage = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close feedback", modifier = Modifier.size(16.dp))
                        }
                    }
                }
                
                LaunchedEffect(msg) {
                    kotlinx.coroutines.delay(3000)
                    if (feedbackMessage == msg) feedbackMessage = null
                }
            }
        }

        // Table container card
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // Table Sticky Header 
                stickyHeader {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(bottom = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Header: Name
                        Row(
                            modifier = Modifier
                                .weight(2.0f)
                                .clickable { toggleSort(ProductSortField.NAME) },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text(
                                "Item (Naam)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            SortIcon(sortField == ProductSortField.NAME, sortAscending)
                        }

                        // Header: Prices
                        Row(
                            modifier = Modifier
                                .weight(1.8f)
                                .clickable { toggleSort(ProductSortField.SELLING_PRICE) },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "Qemat (Kharid / Sell)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            SortIcon(sortField == ProductSortField.SELLING_PRICE, sortAscending)
                        }

                        // Header: Stock
                        Row(
                            modifier = Modifier
                                .weight(1.8f)
                                .clickable { toggleSort(ProductSortField.STOCK) },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "Mojooda (Stock)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            SortIcon(sortField == ProductSortField.STOCK, sortAscending)
                        }

                        // Header: Actions
                        Box(
                            modifier = Modifier.width(108.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Action",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Table Rows
                if (sortedProducts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No items match query.", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                } else {
                    items(sortedProducts, key = { it.id }) { product ->
                        val profit = product.sellingPrice - product.costPrice
                        val isOut = product.stock == 0
                        val isLow = product.stock > 0 && product.stock <= product.minStockThreshold
                        
                        val stockColor = when {
                            isOut -> Color(0xFFD32F2F)
                            isLow -> Color(0xFFD32F2F) // Draw bright red for low stock
                            else -> Color(0xFF2E7D32)
                        }

                        val isCritical = product.stock <= product.minStockThreshold
                        val rowBgColor = if (isCritical) Color(0xFFFFEBEE) else Color.Transparent // Soft light pink-red background alert
                        val rowBorderColor = if (isCritical) {
                            BorderStroke(1.5.dp, Color(0xFFEF9A9A)) // Red highlight border
                        } else {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(rowBgColor)
                                .combinedClickable(
                                    onClick = { onEditProduct(product) },
                                    onLongClick = { 
                                        onIncrementStock(product, 10)
                                        feedbackMessage = "Bulked loaded 10 extra stock units to '${product.name}'!"
                                    }
                                )
                                .border(bottom = rowBorderColor)
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Column 1: Product info (name & category tag)
                            Column(
                                modifier = Modifier
                                    .weight(2.0f)
                                    .padding(end = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = product.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("inventory_item_name_${product.id}")
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = product.category,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                                    ) {
                                        repeat(product.rating) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = Color(0xFFFFB300),
                                                modifier = Modifier.size(8.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Column 2: Kharid / Farokht prices + Margin
                            Column(
                                modifier = Modifier.weight(1.8f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                Text(
                                    text = "Cost: Rs. ${String.format("%.0f", product.costPrice)}",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "Sell: Rs. ${String.format("%.0f", product.sellingPrice)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Margin: Rs. ${String.format("%.0f", profit)}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (profit >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }

                            // Column 3: Stock Badge (Compact & elegant display of Mojooda Stock)
                            Column(
                                modifier = Modifier.weight(1.8f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(stockColor.copy(alpha = 0.12f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${product.stock}",
                                        color = stockColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.testTag("stock_val_${product.id}")
                                    )
                                }
                                Text(
                                    text = if (product.stock == 0) "Khatam (Out)" else "In Stock",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Column 4: Quick Details Edit/More Button
                            Row(
                                modifier = Modifier.width(108.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onGenerateQrCode(product) },
                                    modifier = Modifier
                                        .size(30.dp)
                                        .testTag("qr_product_btn_${product.id}")
                                ) {
                                    Icon(
                                        Icons.Default.QrCode,
                                        contentDescription = "Print/Share QR Tag",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onEditProduct(product) },
                                    modifier = Modifier
                                        .size(30.dp)
                                        .testTag("edit_product_btn_${product.id}")
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit Item details",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onDeleteProduct(product) },
                                    modifier = Modifier
                                        .size(30.dp)
                                        .testTag("delete_product_btn_${product.id}")
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Item",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Table usage tips footer
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Tip: Tap a row header to sort items. Long click a product item row to instant-bundle load 10 stock items directly!",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SortIcon(isActive: Boolean, ascending: Boolean) {
    if (isActive) {
        Icon(
            imageVector = if (ascending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(14.dp)
                .padding(start = 2.dp)
        )
    }
}

// Simple internal border helper
private fun Modifier.border(bottom: BorderStroke): Modifier {
    return this.drawBehind {
        val strokeWidth = bottom.width.toPx()
        val y = size.height - strokeWidth / 2
        val brushColor = (bottom.brush as? SolidColor)?.value ?: Color.LightGray
        drawLine(
            color = brushColor,
            start = androidx.compose.ui.geometry.Offset(0f, y),
            end = androidx.compose.ui.geometry.Offset(size.width, y),
            strokeWidth = strokeWidth
        )
    }
}
