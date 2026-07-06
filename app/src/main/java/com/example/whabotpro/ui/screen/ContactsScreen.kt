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
import com.example.whabotpro.data.model.Contact
import com.example.whabotpro.ui.component.*
import com.example.whabotpro.ui.viewmodel.AppViewModel

@Composable
fun ContactsScreen(vm: AppViewModel, addTrigger: Int = 0) {
    val contacts by vm.contacts.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    // React to toolbar + button
    LaunchedEffect(addTrigger) {
        if (addTrigger > 0) showAdd = true
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        SectionHeader("Contacts", "People who have messaged your WhatsApp")
        if (contacts.isEmpty()) {
            EmptyState("No contacts yet. Contacts are added automatically when someone messages you.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(contacts) { c -> ContactCard(c, vm) }
            }
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Add Contact") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) vm.addContact(Contact(name = name, phoneNumber = phone))
                    showAdd = false
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ContactCard(c: Contact, vm: AppViewModel) {
    CardBox {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(c.name, style = MaterialTheme.typography.titleMedium)
                Text(c.phoneNumber, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (c.notes.isNotEmpty()) Text(c.notes, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { vm.deleteContact(c.id) }) {
                Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
