package com.example.whabotpro.ui.screen

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.whabotpro.ai.FirebaseRawDataSync
import com.example.whabotpro.ai.RawDataProcessor
import com.example.whabotpro.ui.component.CardBox
import com.example.whabotpro.ui.viewmodel.AppViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRawDataScreen(
    vm: AppViewModel,
    onBack: () -> Unit
) {
    var rawText by remember { mutableStateOf("") }
    var processing by remember { mutableStateOf(false) }
    var syncing by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<RawDataProcessor.ProcessResult?>(null) }
    val scope = rememberCoroutineScope()
    val processor = remember { RawDataProcessor() }
    val firebaseSync = remember { FirebaseRawDataSync() }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Raw Data") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Paste raw data, or sync from Firebase. AI will parse and save to the correct tables.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = rawText,
                onValueChange = { rawText = it; result = null },
                label = { Text("Raw Data") },
                placeholder = { Text("Paste menu items, business info, FAQs, policies, etc.\n\nExample:\nRestaurant Name: My Restaurant\nPhone: +92 300 1234567\n\nMenu:\n1. Chicken Karahi - PKR 800 - BBQ\n2. Biryani - PKR 500 - Rice") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                minLines = 10,
                maxLines = 20,
                enabled = !processing && !syncing
            )

            Spacer(Modifier.height(12.dp))

            // Row 1: Paste | Sync Firebase
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = clipboard.primaryClip
                        if (clip != null && clip.itemCount > 0) {
                            val pasted = clip.getItemAt(0).coerceToText(context).toString()
                            if (pasted.isNotBlank()) {
                                rawText = pasted
                                result = null
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !processing && !syncing
                ) {
                    Icon(Icons.Filled.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Paste")
                }
                OutlinedButton(
                    onClick = {
                        syncing = true
                        statusText = "Fetching from Firebase..."
                        scope.launch {
                            val docs = firebaseSync.fetchPending()
                            if (docs.isEmpty()) {
                                statusText = "No pending documents in Firebase"
                                syncing = false
                            } else {
                                statusText = "Found ${docs.size} document(s). Processing..."
                                val allContent = docs.joinToString("\n\n---\n\n") { it.content }
                                rawText = allContent
                                syncing = false
                                statusText = "Loaded ${docs.size} document(s) from Firebase"
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !processing && !syncing
                ) {
                    Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Sync Firebase")
                }
                OutlinedButton(
                    onClick = { rawText = ""; result = null; statusText = "" },
                    modifier = Modifier.weight(1f),
                    enabled = !processing && !syncing && rawText.isNotEmpty()
                ) {
                    Text("Clear")
                }
            }

            Spacer(Modifier.height(12.dp))

            // Row 2: Process with AI (full width)
            Button(
                onClick = {
                    if (rawText.isNotBlank()) {
                        processing = true
                        result = null
                        statusText = "Sending data to AI..."
                        scope.launch {
                            result = processor.process(rawText)
                            processing = false
                            // If we loaded from Firebase, mark docs as processed
                            if (result?.success == true) {
                                statusText = "Saved ${result!!.savedCount} items"
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !processing && !syncing && rawText.isNotBlank()
            ) {
                Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Process with AI")
            }

            Spacer(Modifier.height(16.dp))

            // Status / Progress
            if (syncing || processing) {
                CardBox {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Column {
                            Text(statusText.ifEmpty { if (processing) "AI is processing..." else "Syncing..." })
                            Text(
                                "Data size: ${rawText.length} chars",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Status message (non-progress)
            if (statusText.isNotBlank() && !syncing && !processing) {
                CardBox {
                    Text(statusText, style = MaterialTheme.typography.bodyMedium)
                }
            }

            result?.let { res ->
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (res.success)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            if (res.success) "Saved ${res.savedCount} items" else "Processing failed",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("Engine: ${res.engineUsed}", style = MaterialTheme.typography.bodySmall)
                        if (res.details.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(res.details, style = MaterialTheme.typography.bodySmall)
                        }
                        if (!res.success && res.aiResponse.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text("AI Response: ${res.aiResponse.take(500)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                if (res.success) {
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Done — Back to Dashboard")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
