package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.GeminiPriceService
import kotlinx.coroutines.launch
import java.util.Locale

class CalculatorState {
    var display by mutableStateOf("")
    var memory by mutableStateOf("")
    var operator by mutableStateOf("")

    fun onDigit(digit: String) {
        if (display == "Error" || display == "0") {
            display = digit
        } else {
            // limit size
            if (display.length < 12) {
                display += digit
            }
        }
    }

    fun onOperator(op: String) {
        if (display.isNotEmpty() && display != "Error") {
            if (memory.isNotEmpty() && operator.isNotEmpty()) {
                calculate()
            }
            memory = display
            operator = op
            display = ""
        }
    }

    fun calculate() {
        if (memory.isNotEmpty() && display.isNotEmpty() && operator.isNotEmpty()) {
            val num1 = memory.toDoubleOrNull() ?: 0.0
            val num2 = display.toDoubleOrNull() ?: 0.0
            val res = when (operator) {
                "+" -> num1 + num2
                "-" -> num1 - num2
                "*" -> num1 * num2
                "/" -> if (num2 != 0.0) num1 / num2 else Double.NaN
                else -> 0.0
            }
            display = formatResult(res)
            memory = ""
            operator = ""
        }
    }

    fun onPercent() {
        val num = display.toDoubleOrNull()
        if (num != null) {
            display = formatResult(num / 100.0)
        }
    }

    fun onMarkup(percent: Double) {
        val num = display.toDoubleOrNull()
        if (num != null && num != 0.0) {
            display = formatResult(num * (1.0 + percent))
        }
    }

    fun onDiscount(percent: Double) {
        val num = display.toDoubleOrNull()
        if (num != null && num != 0.0) {
            display = formatResult(num * (1.0 - percent))
        }
    }

    fun onBackspace() {
        if (display.isNotEmpty() && display != "Error") {
            display = display.substring(0, display.length - 1)
        } else {
            display = ""
        }
    }

    fun onClear() {
        display = ""
        memory = ""
        operator = ""
    }

    private fun formatResult(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "Error"
        return if (value % 1 == 0.0) {
            if (value >= Long.MAX_VALUE.toDouble() || value <= Long.MIN_VALUE.toDouble()) {
                value.toString()
            } else {
                value.toLong().toString()
            }
        } else {
            String.format(Locale.US, "%.2f", value)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DukanCalculatorBottomSheet(
    onDismissRequest: () -> Unit
) {
    val calc = remember { CalculatorState() }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hisaab Calculator",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "فوری حساب کتاب اور مارک اپ کیلکولیٹر",
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

            // Quick Markup/Discount Helpers Row
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Profit:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    
                    val profitRates = listOf(0.05, 0.10, 0.15, 0.20)
                    profitRates.forEach { rate ->
                        TextButton(
                            onClick = { calc.onMarkup(rate) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("+${(rate * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(16.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    Text(
                        text = "Discount:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    
                    val discountRates = listOf(0.05, 0.10)
                    discountRates.forEach { rate ->
                        TextButton(
                            onClick = { calc.onDiscount(rate) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("-${(rate * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Display Output
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    // Running formula / Memory
                    val formulaText = if (calc.memory.isNotEmpty() && calc.operator.isNotEmpty()) {
                        "${calc.memory} ${calc.operator}"
                    } else ""
                    Text(
                        text = formulaText,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.End
                    )

                    // Main display value
                    Text(
                        text = calc.display.ifEmpty { "0" },
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        modifier = Modifier.testTag("calculator_display")
                    )
                }
            }

            // Pad Layout
            val buttons = listOf(
                "C", "⌫", "%", "/",
                "7", "8", "9", "*",
                "4", "5", "6", "-",
                "1", "2", "3", "+",
                "0", ".", "", "="
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(buttons) { symbol ->
                    if (symbol.isEmpty()) {
                        Box(modifier = Modifier.height(52.dp))
                    } else {
                        val isOperator = symbol in listOf("/", "*", "-", "+", "=")
                        val isSpecial = symbol in listOf("C", "⌫", "%")
                        
                        val containerColor = when {
                            symbol == "=" -> MaterialTheme.colorScheme.primary
                            isOperator -> MaterialTheme.colorScheme.primaryContainer
                            isSpecial -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                        }

                        val contentColor = when {
                            symbol == "=" -> MaterialTheme.colorScheme.onPrimary
                            isOperator -> MaterialTheme.colorScheme.onPrimaryContainer
                            isSpecial -> MaterialTheme.colorScheme.onTertiaryContainer
                            else -> MaterialTheme.colorScheme.onSurface
                        }

                        Button(
                            onClick = {
                                when (symbol) {
                                    "C" -> calc.onClear()
                                    "⌫" -> calc.onBackspace()
                                    "%" -> calc.onPercent()
                                    "=" -> calc.calculate()
                                    in listOf("/", "*", "-", "+") -> calc.onOperator(symbol)
                                    else -> calc.onDigit(symbol)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = containerColor,
                                contentColor = contentColor
                            ),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("calc_btn_$symbol")
                        ) {
                            Text(
                                text = symbol,
                                fontSize = if (isOperator || isSpecial) 18.sp else 21.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlinePriceBottomSheet(
    onDismissRequest: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val quickSearches = listOf(
        "Ghee per kg (گھی)",
        "Sugar 1kg (چینی)",
        "Tapal Tea (چائے)",
        "Dal Chana (دال چنا)",
        "Milk Pack (دودھ)",
        "Atta 10kg (آٹا)"
    )

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AI Rate Assistant",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "آن لائن قیمت اور ہول سیل ریٹس معلومات",
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

            // Short Description
            Text(
                text = "کچھ بھی لکھیں (جیسے 'Sufi Oil 1L' یا 'Dal Mong') اور آن لائن مارکیٹ ریٹس کا ایک اچھے اندازہ حاصل کریں۔",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                lineHeight = 16.sp
            )

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Product name likhein... (e.g. Rice 1kg)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("online_price_input"),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // Search / Run Action
            Button(
                onClick = {
                    if (searchQuery.isNotBlank() && !isLoading) {
                        isLoading = true
                        resultText = null
                        scope.launch {
                            resultText = GeminiPriceService.fetchOnlinePrice(searchQuery)
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("search_price_button"),
                enabled = searchQuery.isNotBlank() && !isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Check Online Rate (آن لائن قیمت دیکھیں)", fontWeight = FontWeight.Bold)
                }
            }

            // Quick Suggestion Pills
            if (!isLoading && resultText == null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Quick Search Suggestions:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickSearches.take(3).forEach { p ->
                            SuggestionChip(
                                onClick = { 
                                    searchQuery = p.substringBefore(" (")
                                },
                                label = { Text(p, fontSize = 11.sp) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickSearches.takeLast(3).forEach { p ->
                            SuggestionChip(
                                onClick = { 
                                    searchQuery = p.substringBefore(" (")
                                },
                                label = { Text(p, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }

            // Results / Loading state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                    .padding(16.dp)
            ) {
                if (isLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp))
                        Text(
                            text = "Searching online market databases...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "مارکیٹ ریٹس کا تقابل اور معلومات تلاش کی جا رہی ہیں",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (resultText != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Market Rate Information:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            IconButton(
                                onClick = {
                                    // simple copy action mock or text reset
                                    resultText = null
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Text(
                            text = resultText!!,
                            fontSize = 13.sp,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    // No query yet
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "Enter product and check prices.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "بکری کی چیزیں لکھ کر کرنٹ ریٹس حاصل کریں۔",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
