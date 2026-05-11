package com.mmids.ui.screens

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmids.ui.theme.*

@Composable
fun ConsentScreen(onAgree: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showExitDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))

            // ── App Icon + Title ──────────────────────────────────
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF0D1B2A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Security,
                    contentDescription = null,
                    tint = Blue,
                    modifier = Modifier.size(44.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "MMID",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )
            Text(
                "Manual Mobile Intrusion Detection",
                color = TextSecondary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            // ── Consent Card ──────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BgCard)
                    .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(16.dp))
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F1A26))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Security, contentDescription = null,
                        tint = Green, modifier = Modifier.size(18.dp))
                    Text(
                        "User Consent & Disclosure",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Divider(color = Color.White.copy(0.05f))

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ConsentSection("What This App Does") {
                        ConsentBullet("Monitors app usage and device activity during active sessions")
                        ConsentBullet("Detects and logs potential security events")
                        ConsentBullet("Stores all activity logs locally on your device only")
                        ConsentBullet("Provides psychological deterrence via alerts when monitoring is ON")
                    }

                    ConsentSection("Permissions Required") {
                        ConsentText(
                            "The app may request access to Usage Stats, Device Admin, " +
                            "Notifications, and Storage. Each permission is used strictly " +
                            "for its stated functionality."
                        )
                    }

                    ConsentSection("Auto-Start & Background Operation") {
                        ConsentText(
                            "With your consent, the app can start automatically when your " +
                            "phone boots and runs as a background foreground service. " +
                            "You can disable this in Settings at any time."
                        )
                    }

                    ConsentSection("Hidden Launcher Icon") {
                        ConsentText(
                            "After you accept, the MMID launcher icon will be hidden from " +
                            "your home screen. The app continues running in the background. " +
                            "You can re-open it via the secret dialer code: "
                        )
                        Text(
                            "*#66437#",
                            color = Green,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Green.copy(0.1f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    ConsentSection("Lock Screen Trigger") {
                        ConsentText(
                            "While the screen is locked, only the physical volume buttons " +
                            "on the device function as monitoring triggers. Bluetooth audio " +
                            "controls will NOT trigger any action."
                        )
                    }

                    ConsentSection("Your Responsibility") {
                        ConsentText(
                            "You confirm that you own this device or have explicit permission " +
                            "from the device owner to install and operate this app. The developer " +
                            "is not responsible for any unlawful or unethical use of this application."
                        )
                    }

                    ConsentSection("Developer") {
                        ConsentText("Created and developed by Prashant Piyush.")
                        ConsentText("Telegram: @PrashantCyberCore")
                        ConsentText("Email: cybercore.support@rediffmail.com")
                    }
                }

                // Scroll hint
                AnimatedVisibility(
                    visible = scrollState.value < scrollState.maxValue - 50,
                    enter = fadeIn(), exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BgCard)
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("↓ Scroll to read all", color = TextDim, fontSize = 10.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Action Buttons ────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Exit App button
                OutlinedButton(
                    onClick = { showExitDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextSecondary
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Exit App", fontSize = 14.sp)
                }

                // Agree button
                Button(
                    onClick = onAgree,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Green,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Text("I Agree", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // ── Exit confirmation dialog ──────────────────────────────────
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            containerColor = BgElevated,
            shape = RoundedCornerShape(16.dp),
            title = { Text("Exit MMID?", color = TextPrimary) },
            text = {
                Text(
                    "You must agree to the consent to use this app.\n\nAre you sure you want to exit?",
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    // Exit the app completely
                    (context as? Activity)?.finishAffinity()
                }) {
                    Text("Exit", color = Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

// ── Helper Composables ────────────────────────────────────────────
@Composable
private fun ConsentSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            title,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        content()
    }
    Divider(color = Color.White.copy(0.04f))
}

@Composable
private fun ConsentText(text: String) {
    Text(text, color = TextSecondary, fontSize = 11.sp, lineHeight = 17.sp)
}

@Composable
private fun ConsentBullet(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(start = 4.dp)
    ) {
        Text("•", color = Green, fontSize = 11.sp)
        Text(text, color = TextSecondary, fontSize = 11.sp, lineHeight = 17.sp)
    }
}
