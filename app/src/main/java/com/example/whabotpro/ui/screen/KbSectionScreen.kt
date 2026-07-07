package com.example.whabotpro.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.whabotpro.data.model.KbItem
import com.example.whabotpro.ui.component.*
import com.example.whabotpro.ui.viewmodel.AppViewModel

@Composable
fun KbSectionScreen(vm: AppViewModel, section: String, title: String, subtitle: String, addTrigger: Int = 0) {
    val allItems by vm.kbItems.collectAsState()
    val items = allItems.filter { it.section == section }
    var showEditor by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<KbItem?>(null) }
    var detailItem by remember { mutableStateOf<KbItem?>(null) }

    // React to toolbar + button
    LaunchedEffect(addTrigger) {
        if (addTrigger > 0) {
            editingItem = null
            showEditor = true
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (items.isEmpty()) {
            EmptyState("No items yet. Tap + in the toolbar to create one.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
            ) {
                items(items) { item ->
                    KbItemCard(item = item, onClick = { detailItem = item })
                }
            }
        }
    }

    // Detail dialog (view + edit/delete options)
    if (detailItem != null) {
        KbItemDetailDialog(
            item = detailItem!!,
            onDismiss = { detailItem = null },
            onEdit = {
                editingItem = detailItem
                detailItem = null
                showEditor = true
            },
            onDelete = {
                vm.deleteKbItem(detailItem!!.id)
                detailItem = null
            }
        )
    }

    // Editor dialog (add/edit)
    if (showEditor) {
        KbItemEditorDialog(
            item = editingItem,
            section = section,
            vm = vm,
            onDismiss = { showEditor = false },
            onSave = { item ->
                if (editingItem == null) vm.addKbItem(item)
                else vm.updateKbItem(item)
                showEditor = false
            }
        )
    }
}

@Composable
private fun KbItemCard(item: KbItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (item.content.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(item.content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            if (item.description.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (item.price.isNotEmpty()) {
                    Text("Price: ${item.price}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                if (item.category.isNotEmpty()) {
                    Text("Category: ${item.category}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun KbItemDetailDialog(
    item: KbItem,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.title, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (item.content.isNotEmpty()) {
                    Text(item.content, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                }
                if (item.description.isNotEmpty()) {
                    Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (item.price.isNotEmpty()) {
                        Text("Price: ${item.price}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    if (item.category.isNotEmpty()) {
                        Text("Category: ${item.category}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onEdit) { Text("Edit") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KbItemEditorDialog(
    item: KbItem?,
    section: String,
    vm: AppViewModel,
    onDismiss: () -> Unit,
    onSave: (KbItem) -> Unit
) {
    val editing = item ?: KbItem(section = section)
    var title by remember { mutableStateOf(editing.title) }
    var content by remember { mutableStateOf(editing.content) }
    var description by remember { mutableStateOf(editing.description) }
    var price by remember { mutableStateOf(editing.price) }
    var category by remember { mutableStateOf(editing.category) }
    var showCategorySheet by remember { mutableStateOf(false) }

    // Categories for this section
    val categories = remember(section) { vm.categoriesBySection(section) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Add Item" else "Edit Item") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Content") }, modifier = Modifier.fillMaxWidth(), maxLines = 4)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                // Category selector — tappable, opens bottom sheet
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCategorySheet = true }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Category") },
                        placeholder = { Text("Tap to select category") },
                        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                editing.title = title
                editing.content = content
                editing.description = description
                editing.price = price
                editing.category = category
                editing.updatedAt = System.currentTimeMillis()
                onSave(editing)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    // ── Category Bottom Sheet ──
    if (showCategorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showCategorySheet = false }
        ) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    "Select Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
                HorizontalDivider()

                if (categories.isEmpty()) {
                    // No categories yet — allow creating one inline
                    var newCatName by remember { mutableStateOf("") }
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            "No categories yet for this section.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = newCatName,
                            onValueChange = { newCatName = it },
                            label = { Text("New category name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (newCatName.isNotBlank()) {
                                    val cat = com.example.whabotpro.data.model.Category(
                                        section = section,
                                        name = newCatName.trim()
                                    )
                                    vm.addCategory(cat)
                                    category = newCatName.trim()
                                    showCategorySheet = false
                                }
                            },
                            enabled = newCatName.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Add & Select") }
                    }
                } else {
                    // Show existing categories as a list
                    categories.forEach { cat ->
                        ListItem(
                            headlineContent = { Text(cat.name) },
                            leadingContent = {
                                Icon(Icons.Filled.Label, contentDescription = null)
                            },
                            trailingContent = {
                                if (category == cat.name) {
                                    Icon(Icons.Filled.Check, contentDescription = "Selected")
                                }
                            },
                            modifier = Modifier.clickable {
                                category = cat.name
                                showCategorySheet = false
                            }
                        )
                    }
                    HorizontalDivider()
                    // Option to add a new category
                    var newCatName by remember { mutableStateOf("") }
                    var showAddField by remember { mutableStateOf(false) }
                    if (showAddField) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            OutlinedTextField(
                                value = newCatName,
                                onValueChange = { newCatName = it },
                                label = { Text("New category name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            Row {
                                TextButton(onClick = { showAddField = false; newCatName = "" }) { Text("Cancel") }
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (newCatName.isNotBlank()) {
                                            val cat = com.example.whabotpro.data.model.Category(
                                                section = section,
                                                name = newCatName.trim()
                                            )
                                            vm.addCategory(cat)
                                            category = newCatName.trim()
                                            showCategorySheet = false
                                        }
                                    },
                                    enabled = newCatName.isNotBlank()
                                ) { Text("Add") }
                            }
                        }
                    } else {
                        ListItem(
                            headlineContent = { Text("+ Add new category") },
                            leadingContent = { Icon(Icons.Filled.Add, contentDescription = null) },
                            modifier = Modifier.clickable { showAddField = true }
                        )
                    }
                }
            }
        }
    }
}
