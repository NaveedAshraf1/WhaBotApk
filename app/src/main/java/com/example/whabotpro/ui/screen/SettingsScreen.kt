package com.example.whabotpro.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.whabotpro.BuildConfig
import com.example.whabotpro.data.model.AppSettings
import com.example.whabotpro.ui.component.CardBox
import com.example.whabotpro.ui.component.SectionHeader
import com.example.whabotpro.ui.viewmodel.AppViewModel

@Composable
fun SettingsScreen(vm: AppViewModel, context: android.content.Context) {
    val settings by vm.settings.collectAsState()
    val waState by vm.waState.collectAsState()

    var apiKey by remember(settings) { mutableStateOf(settings.groqApiKey) }
    var model by remember(settings) { mutableStateOf(settings.groqModel) }
    var geminiKey by remember(settings) { mutableStateOf(settings.geminiApiKey) }
    var geminiModel by remember(settings) { mutableStateOf(settings.geminiModel) }
    var autoReply by remember(settings) { mutableStateOf(settings.autoReplyEnabled) }
    var businessName by remember(settings) { mutableStateOf(settings.businessName) }
    var baileysUrl by remember(settings) { mutableStateOf(settings.baileysServerUrl) }
    var showKey by remember { mutableStateOf(false) }
    var showGeminiKey by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        // Service control
        CardBox {
            Text("Service", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("WhatsApp: ${waState.name}", style = MaterialTheme.typography.bodySmall)
            Text("Server: Embedded (port 3001)", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            Row {
                Button(onClick = { vm.startService(context) }) { Text("Start Service") }
                Spacer(Modifier.width(12.dp))
                OutlinedButton(onClick = { vm.stopService(context) }) { Text("Stop Service") }
            }
        }

        Spacer(Modifier.height(16.dp))

        // AI settings
        CardBox {
            Text("AI Configuration", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("Groq API Key") },
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { showKey = !showKey }) { Text(if (showKey) "Hide" else "Show") }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("Groq Model") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text("Get a free Groq key at console.groq.com/keys", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(16.dp))
            Text("Gemini (auto-configured fallback)", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = geminiKey,
                onValueChange = { geminiKey = it },
                label = { Text("Gemini API Key") },
                singleLine = true,
                visualTransformation = if (showGeminiKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { showGeminiKey = !showGeminiKey }) { Text(if (showGeminiKey) "Hide" else "Show") }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = geminiModel,
                onValueChange = { geminiModel = it },
                label = { Text("Gemini Model") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(16.dp))

        // Bot settings
        CardBox {
            Text("Bot Settings", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = businessName,
                onValueChange = { businessName = it },
                label = { Text("Business Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = baileysUrl,
                onValueChange = { baileysUrl = it },
                label = { Text("Baileys Server URL") },
                placeholder = { Text("http://192.168.1.100:3000") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Row {
                Text("Auto-Reply", modifier = Modifier.weight(1f))
                Switch(checked = autoReply, onCheckedChange = { autoReply = it })
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            vm.saveSettings(AppSettings(
                groqApiKey = apiKey.trim(),
                groqModel = model.trim().ifEmpty { "llama-3.1-8b-instant" },
                geminiApiKey = geminiKey.trim().ifEmpty { BuildConfig.GEMINI_API_KEY },
                geminiModel = geminiModel.trim().ifEmpty { "gemini-2.5-flash" },
                autoReplyEnabled = autoReply,
                businessName = businessName,
                baileysServerUrl = baileysUrl.trim().ifEmpty { "http://127.0.0.1:3001" }
            ))
            saved = true
        }) { Text("Save Settings") }

        if (saved) {
            Spacer(Modifier.height(8.dp))
            Text("Settings saved!", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }
    }
}
