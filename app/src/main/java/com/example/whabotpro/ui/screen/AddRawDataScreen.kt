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

private fun parseSectionCounts(details: String): Map<String, Int> {
    val counts = mutableMapOf<String, Int>()
    val lines = details.lines()
    
    for (line in lines) {
        when {
            line.contains("category") -> counts["Categories"] = (counts["Categories"] ?: 0) + 1
            line.contains("(menu)") -> counts["Menu Items"] = (counts["Menu Items"] ?: 0) + 1
            line.contains("(promotions)") -> counts["Deals"] = (counts["Deals"] ?: 0) + 1
            line.contains("(services)") -> counts["Services"] = (counts["Services"] ?: 0) + 1
            line.contains("(faqs)") -> counts["FAQs"] = (counts["FAQs"] ?: 0) + 1
            line.contains("(policies)") -> counts["Policies"] = (counts["Policies"] ?: 0) + 1
            line.contains("(events)") -> counts["Events"] = (counts["Events"] ?: 0) + 1
            line.contains("(reservations)") -> counts["Reservations"] = (counts["Reservations"] ?: 0) + 1
            line.contains("(delivery_zones)") -> counts["Delivery Zones"] = (counts["Delivery Zones"] ?: 0) + 1
            line.contains("Business info") -> counts["Business Info"] = (counts["Business Info"] ?: 0) + 1
            line.contains("rule") -> counts["Rules"] = (counts["Rules"] ?: 0) + 1
            line.contains("contact") -> counts["Contacts"] = (counts["Contacts"] ?: 0) + 1
            line.contains("order") -> counts["Orders"] = (counts["Orders"] ?: 0) + 1
        }
    }
    
    return counts
}

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
    var showAiResponseDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val processor = remember { RawDataProcessor() }
    val firebaseSync = remember { FirebaseRawDataSync() }
    val context = LocalContext.current
    
    // Character limit to avoid rate limits and token issues
    val maxChars = 5000
    val isOverLimit = rawText.length > maxChars

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
                onValueChange = { 
                    if (it.length <= maxChars) {
                        rawText = it
                        result = null
                    }
                },
                label = { Text("Raw Data") },
                placeholder = { Text("Paste menu items, business info, FAQs, policies, etc.\n\nExample:\nRestaurant Name: My Restaurant\nPhone: +92 300 1234567\n\nMenu:\n1. Chicken Karahi - PKR 800 - BBQ\n2. Biryani - PKR 500 - Rice") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                minLines = 10,
                maxLines = 20,
                enabled = !processing && !syncing,
                supportingText = {
                    Text(
                        "${rawText.length} / $maxChars characters",
                        color = if (isOverLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                isError = isOverLimit
            )

            if (isOverLimit) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Data exceeds $maxChars character limit. Please reduce the content.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

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

            // Row 2: Save with AI (full width)
            Button(
                onClick = {
                    if (rawText.isNotBlank()) {
                        processing = true
                        result = null
                        statusText = "Sending data to AI..."
                        scope.launch {
                            result = processor.process(rawText)
                            processing = false
                            if (result?.success == true) {
                                statusText = "Saved ${result!!.savedCount} items"
                            } else {
                                statusText = "Processing failed"
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !processing && !syncing && rawText.isNotBlank() && !isOverLimit
            ) {
                Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Save with AI")
            }

            Spacer(Modifier.height(16.dp))

            // Status / Progress (inline card)
            if (syncing) {
                CardBox {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Column {
                            Text(statusText.ifEmpty { "Syncing..." })
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
                            if (res.success) "Data Imported Successfully" else "Processing Failed",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("Engine: ${res.engineUsed}", style = MaterialTheme.typography.bodySmall)
                        
                        if (res.success) {
                            Spacer(Modifier.height(12.dp))
                            // Parse details to show section breakdown
                            val sectionCounts = parseSectionCounts(res.details)
                            if (sectionCounts.isNotEmpty()) {
                                Text("Items Added:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(8.dp))
                                sectionCounts.forEach { (section, count) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(section, style = MaterialTheme.typography.bodySmall)
                                        Text("$count", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                }
                                Spacer(Modifier.height(8.dp))
                                Text("Total: ${res.savedCount} items", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            Spacer(Modifier.height(8.dp))
                            Text(res.details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            if (res.aiResponse.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text("AI Response:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(4.dp))
                                Text(res.aiResponse.take(800), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { showAiResponseDialog = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("View Full AI Response")
                                }
                            }
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

    // Progress dialog overlay when AI is processing
    if (processing) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = { },
            title = { Text("Processing with AI") },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    Column {
                        Text(statusText.ifEmpty { "AI is parsing your data..." })
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Data size: ${rawText.length} chars",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        )
    }

    // AI Response dialog for debugging failures
    if (showAiResponseDialog && result != null) {
        AlertDialog(
            onDismissRequest = { showAiResponseDialog = false },
            title = { Text("Full AI Response") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        result!!.aiResponse,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = android.content.ClipData.newPlainText("AI Response", result!!.aiResponse)
                        clipboard.setPrimaryClip(clip)
                        showAiResponseDialog = false
                    }
                ) {
                    Text("Copy")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAiResponseDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
