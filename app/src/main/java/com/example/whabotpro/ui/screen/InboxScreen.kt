package com.example.whabotpro.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.whabotpro.data.model.InboxMessage
import com.example.whabotpro.ui.component.CardBox
import com.example.whabotpro.ui.component.EmptyState
import com.example.whabotpro.ui.component.SectionHeader
import com.example.whabotpro.ui.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InboxScreen(vm: AppViewModel) {
    val inbox by vm.inbox.collectAsState()
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                SectionHeader("Inbox", "Recent incoming and outgoing messages")
            }
            if (inbox.isNotEmpty()) {
                TextButton(onClick = { vm.clearInbox() }) { Text("Clear All") }
            }
        }

        if (inbox.isEmpty()) {
            EmptyState("No messages yet. Incoming WhatsApp messages will appear here.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
            ) {
                items(inbox) { msg ->
                    InboxItem(msg, dateFormat)
                }
            }
        }
    }
}

@Composable
private fun InboxItem(msg: InboxMessage, dateFormat: SimpleDateFormat) {
    val isIn = msg.direction == "in"
    CardBox {
        Row(modifier = Modifier.fillMaxWidth()) {
            Icon(
                if (isIn) Icons.Filled.CallReceived else Icons.Filled.CallMade,
                contentDescription = null,
                tint = if (isIn) MaterialTheme.colorScheme.primary else Color(0xFF25D366),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        msg.contactName.ifEmpty { msg.phoneNumber },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        dateFormat.format(Date(msg.timestamp)),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(msg.body, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Row {
                    Text(
                        if (isIn) "Incoming" else "Outgoing",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(" • ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Source: ${msg.source}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
