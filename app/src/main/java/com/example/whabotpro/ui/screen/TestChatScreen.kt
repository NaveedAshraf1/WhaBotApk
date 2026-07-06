package com.example.whabotpro.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whabotpro.data.model.ChatHistoryEntry
import com.example.whabotpro.ui.component.SectionHeader
import com.example.whabotpro.ui.viewmodel.AppViewModel
import kotlinx.coroutines.launch

@Composable
fun TestChatScreen(vm: AppViewModel) {
    val messages by vm.testChatMessages.collectAsState()
    val loading by vm.testChatLoading.collectAsState()
    val settings by vm.settings.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        SectionHeader("Test Chat", "Test the AI agent without sending real WhatsApp messages")
        if (settings.groqApiKey.isEmpty() && settings.geminiApiKey.isEmpty()) {
            Text("Set Groq or Gemini API key in Settings first.", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(msg)
            }
            if (loading) {
                item { CircularProgressIndicator(modifier = Modifier.size(20.dp).padding(8.dp)) }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Type a message...") },
                modifier = Modifier.weight(1f),
                maxLines = 3
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (input.isNotBlank() && !loading) {
                        vm.sendTestMessage(input.trim())
                        input = ""
                    }
                },
                enabled = input.isNotBlank() && !loading
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun ChatBubble(msg: ChatHistoryEntry) {
    val isUser = msg.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                msg.content,
                modifier = Modifier.padding(12.dp),
                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp
            )
        }
    }
}
