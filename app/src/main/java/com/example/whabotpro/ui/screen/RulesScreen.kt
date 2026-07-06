package com.example.whabotpro.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.whabotpro.data.model.Rule
import com.example.whabotpro.ui.component.*
import com.example.whabotpro.ui.viewmodel.AppViewModel

@Composable
fun RulesScreen(vm: AppViewModel, addTrigger: Int = 0) {
    val rules by vm.rules.collectAsState()
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Rule?>(null) }

    // React to toolbar + button
    LaunchedEffect(addTrigger) {
        if (addTrigger > 0) {
            editing = null
            showEditor = true
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        SectionHeader("Rules", "Custom rules the AI must follow when replying")
        if (rules.isEmpty()) {
            EmptyState("No rules yet. Add rules to guide the AI's behavior.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(rules) { r -> RuleCard(r, vm) { editing = r; showEditor = true } }
            }
        }
    }

    if (showEditor) {
        val e = editing ?: Rule()
        var title by remember { mutableStateOf(e.title) }
        var content by remember { mutableStateOf(e.content) }
        var active by remember { mutableStateOf(e.active) }
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text(if (editing == null) "Add Rule" else "Edit Rule") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Content") }, modifier = Modifier.fillMaxWidth(), maxLines = 4)
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Text("Active", modifier = Modifier.weight(1f))
                        Switch(checked = active, onCheckedChange = { active = it })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    e.title = title; e.content = content; e.active = active
                    if (editing == null) vm.addRule(e) else vm.updateRule(e)
                    showEditor = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEditor = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun RuleCard(r: Rule, vm: AppViewModel, onEdit: () -> Unit) {
    CardBox {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(r.title, style = MaterialTheme.typography.titleMedium)
                if (r.content.isNotEmpty()) Text(r.content, style = MaterialTheme.typography.bodyMedium)
                Text(if (r.active) "Active" else "Inactive", style = MaterialTheme.typography.labelSmall, color = if (r.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column {
                TextButton(onClick = onEdit) { Text("Edit") }
                IconButton(onClick = { vm.deleteRule(r.id) }) {
                    Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
