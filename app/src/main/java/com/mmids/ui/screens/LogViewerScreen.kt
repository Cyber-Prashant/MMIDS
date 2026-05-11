package com.mmids.ui.screens

import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmids.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var logContent by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        logContent = readLogs(context)
    }

    val sessionCount = remember(logContent) {
        Regex("SESSION START").findAll(logContent).count()
    }
    val appCount = remember(logContent) {
        Regex("\\[APP OPENED\\]").findAll(logContent).count()
    }
    val isEmpty = logContent.trim().isEmpty()

    Scaffold(
        containerColor = BgPrimary,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgPrimary),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Terminal, contentDescription = null,
                            tint = Green, modifier = Modifier.size(18.dp))
                        Text("Activity Logs", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = { logContent = readLogs(context) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = TextSecondary)
                    }
                    IconButton(
                        onClick = { showClearDialog = true },
                        enabled = !isEmpty
                    ) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Clear",
                            tint = if (isEmpty) TextDim else Red)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // ── Stats Bar ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F0F0F))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatChip(icon = Icons.Outlined.FolderOpen,
                    label = "$sessionCount Sessions", color = Blue)
                StatChip(icon = Icons.Outlined.Apps,
                    label = "$appCount App Events", color = Orange)
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Green.copy(0.1f))
                        .border(1.dp, Green.copy(0.3f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = null,
                        tint = Green, modifier = Modifier.size(11.dp))
                    Text("Hidden Storage", color = Green, fontSize = 10.sp)
                }
            }

            // ── Terminal Log Display ──────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF080808))
                    .border(1.dp, Green.copy(0.2f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                val scrollState = rememberScrollState()
                Text(
                    text = if (isEmpty) "No session logs found." else logContent,
                    color = if (isEmpty) TextDim else TextTerminal,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                )
            }

            // ── Clear Button ──────────────────────────────────────
            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                Button(
                    onClick = { showClearDialog = true },
                    enabled = !isEmpty,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Red.copy(0.12f),
                        contentColor = Red,
                        disabledContainerColor = BgCard,
                        disabledContentColor = TextDim
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isEmpty) BgDivider else Red.copy(0.3f)),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Icon(Icons.Outlined.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Clear All Logs", fontSize = 14.sp)
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = BgElevated,
            title = { Text("Clear All Logs?", color = TextPrimary) },
            text = { Text("This will permanently delete all session logs.\nThis action cannot be undone.",
                color = TextSecondary, lineHeight = 20.sp) },
            confirmButton = {
                TextButton(onClick = {
                    clearLogs(context)
                    logContent = ""
                    showClearDialog = false
                }) { Text("Delete All", color = Red) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun StatChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(label, color = color, fontSize = 12.sp)
    }
}

fun readLogs(context: Context): String {
    return try {
        val f = java.io.File(java.io.File(context.filesDir, ".mmids_logs"), "session_log.txt")
        if (f.exists()) f.readText() else ""
    } catch (_: Exception) { "" }
}
