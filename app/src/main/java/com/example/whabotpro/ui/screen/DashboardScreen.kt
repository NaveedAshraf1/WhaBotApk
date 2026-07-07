package com.example.whabotpro.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.example.whabotpro.data.model.WaState
import com.example.whabotpro.ui.component.*
import com.example.whabotpro.ui.viewmodel.AppViewModel

@Composable
fun DashboardScreen(vm: AppViewModel, navTo: (Screen) -> Unit) {
    val waState by vm.waState.collectAsState()
    val settings by vm.settings.collectAsState()
    val inbox by vm.inbox.collectAsState()
    val businessInfo by vm.businessInfo.collectAsState()

    val aiConfigured = settings.groqApiKey.isNotEmpty() || settings.geminiApiKey.isNotEmpty()
    val aiLabel = when {
        settings.groqApiKey.isNotEmpty() -> settings.groqModel
        settings.geminiApiKey.isNotEmpty() -> settings.geminiModel
        else -> "Not configured"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Status cards
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.QrCodeScanner,
                label = "WhatsApp",
                value = when (waState) {
                    WaState.CONNECTED -> "Connected"
                    WaState.QR_READY -> "QR Ready"
                    WaState.CODE_READY -> "Code Ready"
                    WaState.CONNECTING -> "Connecting"
                    WaState.ERROR -> "Error"
                    WaState.DISCONNECTED -> "Off"
                },
                color = if (waState == WaState.CONNECTED) Color(0xFF25D366) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            StatusCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Psychology,
                label = "AI Engine",
                value = aiLabel,
                color = if (aiConfigured) Color(0xFF25D366) else MaterialTheme.colorScheme.error
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Dns,
                label = "Server",
                value = "Running :3001",
                color = Color(0xFF25D366)
            )
            StatusCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Inbox,
                label = "Inbox",
                value = "${inbox.size} msgs",
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(24.dp))

        // Business Info card
        if (businessInfo.brandName.isNotBlank()) {
            Text("Business", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            CardBox {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(businessInfo.brandName, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    if (businessInfo.tagline.isNotBlank()) {
                        Text(businessInfo.tagline, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (businessInfo.category.isNotBlank()) {
                        Text(businessInfo.category, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (businessInfo.phone.isNotBlank()) {
                        Text(businessInfo.phone, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (businessInfo.address.isNotBlank()) {
                        Text(businessInfo.address, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // Quick actions
        Text("Quick Actions", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))

        QuickActionGrid(navTo, waState)
    }
}

@Composable
private fun StatusCard(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, color: Color) {
    CardBox(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = color)
            }
        }
    }
}

@Composable
private fun QuickActionGrid(navTo: (Screen) -> Unit, waState: WaState) {
    val actions = listOf(
        Triple(Screen.RawData, "Add Raw Data", Icons.Filled.AutoFixHigh),
        Triple(Screen.Connect, "Connect WhatsApp", Icons.Filled.QrCodeScanner),
        Triple(Screen.TestChat, "Test AI Chat", Icons.Filled.Science)
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        actions.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { (screen, label, icon) ->
                    OutlinedCard(
                        onClick = { navTo(screen) },
                        modifier = Modifier.weight(1f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
