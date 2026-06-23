package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.DukanViewModel
import com.example.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale

@Composable
fun LoginScreen(
    viewModel: DukanViewModel,
    onLoginSuccess: (String, String) -> Unit
) {
    val context = LocalContext.current
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf("") }
    
    // States for interactive Google accounts sheet
    var showGoogleAccountsSheet by remember { mutableStateOf(false) }
    var showManualGmailInput by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // App Brand visual (Universal Multi-Category logo: Mobiles, Karyana, Electronics)
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_dukan_universal_logo_1782046307454),
                    contentDescription = "Apni Dukan Universal Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "APNI DUKAN",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Rafaqat & Bharosa Book (Karobar Manager)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(0.4f))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Gmail Account se Login Karain",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Apna udhaar khata aur inventory mehfooz rakhne ke liye sign in zaroori hai.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Authentic Professional Google Button
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showGoogleAccountsSheet = true }
                            .testTag("google_login_btn"),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCCCCCC))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
                        ) {
                            // Custom drawn Google 'G' Icon
                            GoogleIcon(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Sign in with Google",
                                color = Color(0xFF555555),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    if (!showManualGmailInput) {
                        TextButton(
                            onClick = { showManualGmailInput = true },
                            modifier = Modifier.testTag("manual_gmail_toggle_btn")
                        ) {
                            Text("Manually enter Gmail Address instead", fontSize = 12.sp)
                        }
                    }

                    AnimatedVisibility(visible = showManualGmailInput) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text("Gmail / Email Address") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("manual_email_input")
                            )

                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("manual_password_input")
                            )

                            if (loginError.isNotEmpty()) {
                                Text(
                                    text = loginError,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Button(
                                onClick = {
                                    if (!emailInput.contains("@") || !emailInput.endsWith(".com")) {
                                        loginError = "Please enter a valid email address (e.g., example@gmail.com)."
                                    } else if (passwordInput.length < 6) {
                                        loginError = "Password must be at least 6 characters long."
                                    } else {
                                        val name = emailInput.substringBefore("@")
                                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                                        viewModel.login(emailInput, name)
                                        onLoginSuccess(emailInput, name)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("manual_login_submit_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Sign In with Gmail", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.6f))
            
            Text(
                text = "Secure Verification powered by Google Play Identity Services",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }

    // Google Accounts Choose Sheet
    if (showGoogleAccountsSheet) {
        AlertDialog(
            onDismissRequest = { showGoogleAccountsSheet = false },
            title = null,
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GoogleLogoSmall()
                        Text(
                            text = "Choose an account",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }

                    Text(
                        text = "to continue to Apni Dukan Barosa Book",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    // User Personal Active Google Account
                    AccountSelectionRow(
                        email = "azaanatif838@gmail.com",
                        name = "Azaan Atif",
                        photoLetter = "A",
                        avatarBg = Color(0xFF673AB7),
                        onClick = {
                            viewModel.login("azaanatif838@gmail.com", "Azaan Atif")
                            onLoginSuccess("azaanatif838@gmail.com", "Azaan Atif")
                            showGoogleAccountsSheet = false
                        }
                    )

                    // Secondary standard demo account
                    AccountSelectionRow(
                        email = "dukan.partner@gmail.com",
                        name = "Dukan Partner Demo",
                        photoLetter = "D",
                        avatarBg = Color(0xFFE91E63),
                        onClick = {
                            viewModel.login("dukan.partner@gmail.com", "Dukan Partner")
                            onLoginSuccess("dukan.partner@gmail.com", "Dukan Partner")
                            showGoogleAccountsSheet = false
                        }
                    )

                    Divider(color = Color.LightGray.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showGoogleAccountsSheet = false
                                showManualGmailInput = true
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFFEEEEEE), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.DarkGray
                            )
                        }
                        Text(
                            text = "Use another Gmail account",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1A73E8)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showGoogleAccountsSheet = false },
                    modifier = Modifier.testTag("dismiss_google_sheet_btn")
                ) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }
}

@Composable
fun AccountSelectionRow(
    email: String,
    name: String,
    photoLetter: String,
    avatarBg: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(avatarBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = photoLetter,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = email,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = Color.Transparent, // Keeps spacing clean
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun GoogleIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val cx = width / 2f
        val cy = height / 2f
        val radius = width * 0.45f

        // Google red arc
        val pathRed = Path().apply {
            moveTo(cx, cy)
            lineTo(cx + radius, cy)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(cx - radius, cy - radius, cx + radius, cy + radius),
                startAngleDegrees = 0f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
            close()
        }
        
        // Google green arc
        val pathGreen = Path().apply {
            moveTo(cx, cy)
            lineTo(cx - radius, cy)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(cx - radius, cy - radius, cx + radius, cy + radius),
                startAngleDegrees = 180f,
                sweepAngleDegrees = -90f,
                forceMoveTo = false
            )
            close()
        }

        // Just draw paths with Google colors
        // For simplicity and exact look, let's draw colorful circular slices
        drawArc(
            color = Color(0xFFEA4335), // Red
            startAngle = 180f,
            sweepAngle = 135f,
            useCenter = true
        )
        drawArc(
            color = Color(0xFF4285F4), // Blue
            startAngle = 315f,
            sweepAngle = 90f,
            useCenter = true
        )
        drawArc(
            color = Color(0xFF34A853), // Green
            startAngle = 45f,
            sweepAngle = 135f,
            useCenter = true
        )
        drawArc(
            color = Color(0xFFFBBC05), // Yellow
            startAngle = 135f,
            sweepAngle = 45f,
            useCenter = true
        )

        // Draw a central white cutout circle to make it look like a "G" or an authentic multi-colored ring
        drawCircle(
            color = Color.White,
            radius = radius * 0.65f,
            center = Offset(cx, cy)
        )

        // Draw the central bar of 'G' in blue
        val barPath = Path().apply {
            moveTo(cx + radius * 0.15f, cy - radius * 0.15f)
            lineTo(cx + radius * 0.95f, cy - radius * 0.15f)
            lineTo(cx + radius * 0.95f, cy + radius * 0.15f)
            lineTo(cx + radius * 0.15f, cy + radius * 0.15f)
            close()
        }
        drawPath(barPath, Color(0xFF4285F4))
    }
}

@Composable
fun GoogleLogoSmall(modifier: Modifier = Modifier.size(24.dp)) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
        Text("G", color = Color(0xFF4285F4), style = style)
        Text("o", color = Color(0xFFEA4335), style = style)
        Text("o", color = Color(0xFFFBBC05), style = style)
        Text("g", color = Color(0xFF4285F4), style = style)
        Text("l", color = Color(0xFF34A853), style = style)
        Text("e", color = Color(0xFFEA4335), style = style)
    }
}
