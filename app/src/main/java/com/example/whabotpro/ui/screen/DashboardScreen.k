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
    val connectedUser by vm.connectedUser.collectAsState()
    val settings by vm.settings.collectAsState()
    val inbox by vm.inbox.collectAsState()
    val orders by vm.orders.collectAsState()
    val contacts by vm.contacts.collectAsState()
    val isRequestingPairing by vm.isRequestingPairing.collectAsState()

    val aiConfigured = settings.groqApiKey.isNotEmpty() || settings.geminiApiKey.isNotEmpty()
    val aiLabel = when {
        settings.groqApiKey.isNotEmpty() -> settings.groqModel
        settings.geminiApiKey.isNotEmpty() -> settings.geminiModel
        else -> "Not configured"
    }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        SectionHeader("Dashboard", "Overview of your WhatsApp AI bot")

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

        // Quick actions
        Text("Quick Actions", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))

        // Add Raw Data — prominent full-width button
        OutlinedCard(
            onClick = { navTo(Screen.RawData) },
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.AutoFixHigh, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Add Raw Data", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Paste restaurant data — AI parses & saves to database",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
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
        Triple(Screen.Menu, "Add Data", Icons.Filled.AutoFixHigh),
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
