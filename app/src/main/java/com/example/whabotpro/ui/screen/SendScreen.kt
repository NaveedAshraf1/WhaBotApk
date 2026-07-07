package com.example.whabotpro.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.concurrent.atomic.AtomicInteger
import com.example.whabotpro.ui.component.CardBox
import com.example.whabotpro.ui.component.SectionHeader
import com.example.whabotpro.ui.viewmodel.AppViewModel

@Composable
fun BulkSendScreen(vm: AppViewModel) {
    var numbers by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("")
    }
    var status by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        CardBox {
            OutlinedTextField(
                value = numbers,
                onValueChange = { numbers = it },
                label = { Text("Phone Numbers (one per line)") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                maxLines = 10
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Message") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                maxLines = 6
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val list = numbers.lines().map { it.trim() }.filter { it.isNotBlank() }
                    if (list.isEmpty() || message.isBlank()) {
                        status = "Enter numbers and message"
                    } else {
                        val successCount = AtomicInteger(0)
                        val failCount = AtomicInteger(0)
                        val total = list.size
                        status = "Sending to $total numbers..."
                        list.forEach { num ->
                            vm.sendText(num, message) { ok ->
                                if (ok) successCount.incrementAndGet()
                                else failCount.incrementAndGet()
                                val done = successCount.get() + failCount.get()
                                if (done == total) {
                                    status = "Sent: ${successCount.get()} succeeded, ${failCount.get()} failed"
                                }
                            }
                        }
                        numbers = ""
                        message = ""
                    }
                },
                enabled = numbers.isNotBlank() && message.isNotBlank()
            ) { Text("Send to All") }
            if (status.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(status, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
