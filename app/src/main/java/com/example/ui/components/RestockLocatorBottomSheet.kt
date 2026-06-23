package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.PhoneNumberUtils
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Wholesaler / Market Data structure
data class WholesaleMarket(
    val id: String,
    val name: String,
    val urduName: String,
    val city: String,
    val address: String,
    val queryAddress: String, // query search for Google Maps
    val phone: String,
    val category: String, // Grocery, General, Electronics, Cloth, etc.
    val details: String,
    val rating: Double = 4.5
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestockLocatorBottomSheet(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    
    // Sample high-quality wholesale dealers and markets directory in Pakistan
    val targetMarkets = remember {
        listOf(
            WholesaleMarket(
                id = "1",
                name = "Jodi Bazaar Grocery Wholesalers",
                urduName = "جوڑی بازار ہول سیل مارکیٹ",
                city = "Karachi",
                address = "Jodi Bazaar, Mithadar, Karachi Central",
                queryAddress = "Jodi Bazaar, Karachi",
                phone = "02132439871",
                category = "Grocery / Karyana",
                details = "Pakistan's largest grains, pulses, sugar, rice and ghee wholesale market. Best for bulk food restock.",
                rating = 4.8
            ),
            WholesaleMarket(
                id = "2",
                name = "Shah Alami General Wholesale Market",
                urduName = "شاہ عالمی ہول سیل مارکیٹ",
                city = "Lahore",
                address = "Shah Alami Chowk, Rang Mahal, Old City Lahore",
                queryAddress = "Shah Alami Market, Lahore",
                phone = "03214569871",
                category = "General / Cosmetics",
                details = "The ultimate paradise for general products, cosmetics, plastic items, home accessories. Low wholesale prices.",
                rating = 4.7
            ),
            WholesaleMarket(
                id = "3",
                name = "Raja Bazaar Food & General Distributors",
                urduName = "راجہ بازار ہول سیل",
                city = "Rawalpindi",
                address = "Raja Bazaar, Rawalpindi near Gunjmandi",
                queryAddress = "Raja Bazaar, Rawalpindi",
                phone = "0515554321",
                category = "Grocery / Karyana",
                details = "Major wholesale hub for Northern Punjab. Grains, dry fruits, spices, tea & household consumer goods at direct trade prices.",
                rating = 4.6
            ),
            WholesaleMarket(
                id = "4",
                name = "Karkhano Wholesale Electronics Hub",
                urduName = "کارخانو ہول سیل مارکیٹ",
                city = "Peshawar",
                address = "Karkhano Market, Hayatabad, Peshawar",
                queryAddress = "Karkhano Market, Peshawar",
                phone = "03129876543",
                category = "Electronics / Toys",
                details = "Wholesale imported general store merchandise, electronics, small machinery, and high-margin plastic toys.",
                rating = 4.5
            ),
            WholesaleMarket(
                id = "5",
                name = "Ghalla Mandi Grain Syndicate",
                urduName = "غلہ منڈی ہول سیل ڈسٹریبیوٹر",
                city = "Faisalabad",
                address = "Ghalla Mandi, Sargodha Road, Faisalabad",
                queryAddress = "Ghalla Mandi, Faisalabad",
                phone = "0418543210",
                category = "Grocery / Karyana",
                details = "Top bulk distribution point for pure wheat flour, sugar, pulses, pulses cleaning factories & rice mills traders link.",
                rating = 4.7
            ),
            WholesaleMarket(
                id = "6",
                name = "Bolton Market Plastic & Toys Depot",
                urduName = "بولٹن مارکیٹ ہول سیلرز",
                city = "Karachi",
                address = "Bolton Market, M.A Jinnah Road, Karachi",
                queryAddress = "Bolton Market, Karachi",
                phone = "02132441234",
                category = "General / Cosmetics",
                details = "A massive core trade center for general merchandise, watches, glassware, gift items and stationery supplies.",
                rating = 4.6
            ),
            WholesaleMarket(
                id = "7",
                name = "Badami Bagh Auto parts & Hardware Manufacturers",
                urduName = "بادامی باغ ہارڈویئر سپلائرز",
                city = "Lahore",
                address = "Badami Bagh, Lahore, Punjab",
                queryAddress = "Badami Bagh, Lahore",
                phone = "03009412345",
                category = "Hardware / Tools",
                details = "Hardware items, local shop tools, engine oils, packaging items wholesale vendors cluster.",
                rating = 4.4
            )
        )
    }

    var selectedCityFilter by remember { mutableStateOf("All Cities") }
    var selectedCategoryFilter by remember { mutableStateOf("All Categories") }
    var searchQuery by remember { mutableStateOf("") }

    val cities = remember { listOf("All Cities", "Karachi", "Lahore", "Rawalpindi", "Peshawar", "Faisalabad") }
    val categories = remember { listOf("All Categories", "Grocery / Karyana", "General / Cosmetics", "Electronics / Toys", "Hardware / Tools") }

    val filteredList = remember(selectedCityFilter, selectedCategoryFilter, searchQuery) {
        targetMarkets.filter { market ->
            val matchesCity = selectedCityFilter == "All Cities" || market.city.lowercase() == selectedCityFilter.lowercase()
            val matchesCategory = selectedCategoryFilter == "All Categories" || market.category.contains(selectedCategoryFilter.substringBefore(" /"), ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() || 
                    market.name.contains(searchQuery, ignoreCase = true) || 
                    market.city.contains(searchQuery, ignoreCase = true) || 
                    market.address.contains(searchQuery, ignoreCase = true)
            matchesCity && matchesCategory && matchesSearch
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Maal Refill Supplier Locator",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "اپنے اسٹاک کو دوبارہ بھرنے کے بڑی ہول سیل مارکیٹ تلاشیں!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Search and Filters
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search wholesalors or markets...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth().testTag("supplier_search_bar"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, null)
                        }
                    }
                }
            )

            // Horizontal Filters Row 1: Cities
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Shehar (Choose City):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    cities.take(3).forEach { city ->
                        val isSelected = selectedCityFilter == city
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedCityFilter = city }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = city,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    cities.takeLast(3).forEach { city ->
                        val isSelected = selectedCityFilter == city
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedCityFilter = city }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = city,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Categories list chips
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Category (Maal Line):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedCategoryFilter == "All Categories") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { selectedCategoryFilter = "All Categories" }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "All Store Maal",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedCategoryFilter == "All Categories") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedCategoryFilter == "Grocery / Karyana") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { selectedCategoryFilter = "Grocery / Karyana" }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Karyana (Grocery)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedCategoryFilter == "Grocery / Karyana") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedCategoryFilter == "General / Cosmetics") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { selectedCategoryFilter = "General / Cosmetics" }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "General Store / Cosmetics",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedCategoryFilter == "General / Cosmetics") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedCategoryFilter == "Electronics / Toys") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { selectedCategoryFilter = "Electronics / Toys" }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Electronics & Hardware",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedCategoryFilter == "Electronics / Toys") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Dealer Cards List
            Text(
                text = "${filteredList.size} Wholesale Refill Hubs Found:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.id }) { market ->
                    WholesaleMarketCard(
                        market = market,
                        onNavigateClick = {
                            try {
                                val url = "geo:0,0?q=${Uri.encode(market.queryAddress)}"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                intent.setPackage("com.google.android.apps.maps")
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback web google maps search 
                                try {
                                    val fallbackUrl = "https://www.google.com/maps/search/?api=1&query=${Uri.encode(market.queryAddress)}"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl))
                                    context.startActivity(intent)
                                } catch (err: Exception) {
                                    Toast.makeText(context, "Cannot open Map Navigation", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onCallClick = {
                            try {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${market.phone}"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot dial. Direct Number: ${market.phone}", Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                }
                
                if (filteredList.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.SearchOff, null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                            Text("Oops! Koi Wholesale market nahi mili.", fontWeight = FontWeight.Bold, color = Color.Gray)
                            Text("Selected city ya categories badal kar dubara check karain.", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)
                            Button(onClick = {
                                selectedCityFilter = "All Cities"
                                selectedCategoryFilter = "All Categories"
                                searchQuery = ""
                            }, modifier = Modifier.padding(top = 8.dp)) {
                                Text("Reset Filters")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WholesaleMarketCard(
    market: WholesaleMarket,
    onNavigateClick: () -> Unit,
    onCallClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("market_card_${market.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Stamp Left side
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    val iconVector = when {
                        market.category.contains("Grocery", true) -> Icons.Default.Kitchen
                        market.category.contains("Electronics", true) -> Icons.Default.Bolt
                        market.category.contains("Hardware", true) -> Icons.Default.Build
                        else -> Icons.Default.Store
                    }
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = market.name,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFFC107).copy(alpha = 0.15f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, null, tint = Color(0xFFFFB300), modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(market.rating.toString(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8D6E63))
                            }
                        }
                    }
                    Text(
                        text = market.address,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = market.city,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Text(
                text = market.urduName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            )

            Text(
                text = market.details,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                lineHeight = 15.sp
            )

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Call Wholesale Supplier Action
                Button(
                    onClick = onCallClick,
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Call Supplier", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Map Navigation action 
                Button(
                    onClick = onNavigateClick,
                    modifier = Modifier.weight(1.3f).height(38.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Deikhain / Navigate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
