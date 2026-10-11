
package com.goldlab.management.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.security.MessageDigest
import java.security.SecureRandom

private val Gold = Color(0xFFE0BD59)
private val BrightGold = Color(0xFFFFD978)
private val Black = Color(0xFF08090B)
private val Card = Color(0xFF17191E)
private val Muted = Color(0xFFB9B5AA)

private const val AUTH_PREFS = "snj_local_auth"
private const val PIN_SALT = "pin_salt"
private const val PIN_HASH = "pin_hash"

@Composable
fun AuthGate(content: @Composable () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(AUTH_PREFS, android.content.Context.MODE_PRIVATE)
    }

    var showSplash by remember { mutableStateOf(true) }
    var unlocked by remember { mutableStateOf(false) }
    var pinExists by remember {
        mutableStateOf(prefs.contains(PIN_HASH) && prefs.contains(PIN_SALT))
    }

    LaunchedEffect(Unit) {
        delay(1800)
        showSplash = false
    }

    if (unlocked) {
        content()
        return
    }

    if (showSplash) {
        SplashScreen()
    } else {
        PinScreen(
            isSetup = !pinExists,
            onPinCreated = { pin ->
                val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
                val saltHex = salt.joinToString("") { "%02x".format(it) }
                val hash = hashPin(saltHex, pin)

                val saved = prefs.edit()
                    .putString(PIN_SALT, saltHex)
                    .putString(PIN_HASH, hash)
                    .commit()

                if (saved) {
                    pinExists = true
                    unlocked = true
                }
            },
            onPinVerified = { pin ->
                val salt = prefs.getString(PIN_SALT, null)
                val storedHash = prefs.getString(PIN_HASH, null)

                if (salt != null && storedHash != null) {
                    val enteredHash = hashPin(salt, pin)
                    if (
                        MessageDigest.isEqual(
                            enteredHash.toByteArray(Charsets.UTF_8),
                            storedHash.toByteArray(Charsets.UTF_8)
                        )
                    ) {
                        unlocked = true
                    }
                }
            }
        )
    }
}

private fun hashPin(salt: String, pin: String): String {
    val bytes = MessageDigest.getInstance("SHA-256")
        .digest("$salt:$pin".toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF17130A),
                        Black,
                        Color(0xFF211A0C)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(BrightGold, Gold, Color(0xFF8F6B19))
                        ),
                        RoundedCornerShape(30.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Diamond,
                    contentDescription = "SNJ Gold Lab",
                    tint = Black,
                    modifier = Modifier.size(66.dp)
                )
            }

            Text(
                "SNJ GOLD LAB",
                color = BrightGold,
                fontSize = 29.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )

            Text(
                "GOLD & SILVER LABORATORY",
                color = Muted,
                fontSize = 12.sp,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(25.dp),
                color = Gold,
                strokeWidth = 2.dp
            )
        }
    }
}

@Composable
private fun PinScreen(
    isSetup: Boolean,
    onPinCreated: (String) -> Unit,
    onPinVerified: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Black, Color(0xFF17140D)))
            )
            .imePadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .background(Gold.copy(alpha = 0.13f), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isSetup) Icons.Default.Diamond else Icons.Default.Lock,
                contentDescription = null,
                tint = BrightGold,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(Modifier.height(22.dp))

        Text(
            if (isSetup) "Create Your PIN" else "Welcome Back",
            style = MaterialTheme.typography.headlineMedium,
            color = BrightGold,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            if (isSetup)
                "Set a 4–6 digit PIN to protect your lab records."
            else
                "Enter your PIN to open SNJ Gold Lab.",
            color = Muted,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(26.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Card),
            border = androidx.compose.foundation.BorderStroke(
                1.dp, Gold.copy(alpha = 0.55f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 6 && it.all(Char::isDigit)) {
                            pin = it
                            message = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (isSetup) "New PIN" else "Enter PIN") },
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword
                    ),
                    singleLine = true
                )

                if (isSetup) {
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = {
                            if (it.length <= 6 && it.all(Char::isDigit)) {
                                confirmPin = it
                                message = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Confirm PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword
                        ),
                        singleLine = true
                    )
                }

                if (message.isNotBlank()) {
                    Text(message, color = MaterialTheme.colorScheme.error)
                }

                Button(
                    onClick = {
                        when {
                            pin.length !in 4..6 ->
                                message = "PIN must contain 4 to 6 digits."

                            isSetup && pin != confirmPin ->
                                message = "PINs do not match. Try again."

                            isSetup -> onPinCreated(pin)

                            else -> {
                                onPinVerified(pin)
                                message = "Incorrect PIN. Please try again."
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold,
                        contentColor = Black
                    )
                ) {
                    Text(
                        if (isSetup) "Save PIN & Continue" else "Unlock App",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "SNJ GOLD LAB  •  OFFLINE ACCESS",
            color = Muted,
            fontSize = 11.sp,
            letterSpacing = 1.sp
        )
    }
}
