package com.example.whabotpro.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whabotpro.ui.component.EmptyState
import com.example.whabotpro.ui.component.SectionHeader
import com.example.whabotpro.ui.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LogsScreen(vm: AppViewModel) {
    val logs by vm.logs.collectAsState()
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (logs.isNotEmpty()) {
                TextButton(onClick = { vm.clearLogs() }) { Text("Clear") }
            }
        }
        if (logs.isEmpty()) {
            EmptyState("No logs yet.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(logs) { log ->
                    val color = when (log.level) {
                        "error" -> Color(0xFFFF4444)
                        "info" -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                        Text(dateFormat.format(Date(log.timestamp)), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace)
                        Spacer(Modifier.width(8.dp))
                        Text("[${log.level}] ", fontSize = 11.sp, color = color, fontFamily = FontFamily.Monospace)
                        Text(log.message, fontSize = 11.sp, color = color, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}
