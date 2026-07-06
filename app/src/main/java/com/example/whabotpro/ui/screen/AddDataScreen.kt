package com.example.whabotpro.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whabotpro.data.model.KbItem
import com.example.whabotpro.data.model.Rule
import com.example.whabotpro.ui.component.CardBox
import com.example.whabotpro.ui.component.SectionHeader
import com.example.whabotpro.ui.viewmodel.AppViewModel

// ── Data type definitions ──
// Each type defines which fields to show and how to save
enum class DataType(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Menu("Menu Item", Icons.Filled.RestaurantMenu),
    Deals("Deal / Promotion", Icons.Filled.LocalOffer),
    Services("Service", Icons.Filled.RoomService),
    FAQs("FAQ", Icons.Filled.Help),
    DeliveryZones("Delivery Zone", Icons.Filled.LocalShipping),
    Events("Event", Icons.Filled.Event),
    Reservations("Reservation Info", Icons.Filled.TableRestaurant),
    Policies("Policy", Icons.Filled.Policy),
    Rules("AI Rule", Icons.Filled.Rule),
    Orders("Order", Icons.Filled.ShoppingCart)
}

// Field definitions per data type
data class FieldDef(
    val key: String,
    val label: String,
    val required: Boolean = false,
    val multiline: Boolean = false,
    val isCategory: Boolean = false
)

object FieldSchema {
    // KB-based types share fields but show different subsets
    val menuFields = listOf(
        FieldDef("title", "Item Name", required = true),
        FieldDef("price", "Price", required = true),
        FieldDef("category", "Category", isCategory = true),
        FieldDef("content", "Description / Details", multiline = true),
        FieldDef("available", "Available")
    )

    val dealFields = listOf(
        FieldDef("title", "Deal Title", required = true),
        FieldDef("price", "Price / Discount", required = true),
        FieldDef("category", "Category", isCategory = true),
        FieldDef("content", "Deal Details", multiline = true),
        FieldDef("available", "Available")
    )

    val serviceFields = listOf(
        FieldDef("title", "Service Name", required = true),
        FieldDef("content", "Description", multiline = true),
        FieldDef("category", "Category", isCategory = true),
        FieldDef("available", "Available")
    )

    val faqFields = listOf(
        FieldDef("title", "Question", required = true),
        FieldDef("content", "Answer", required = true, multiline = true)
    )

    val deliveryZoneFields = listOf(
        FieldDef("title", "Area / Zone Name", required = true),
        FieldDef("price", "Delivery Fee"),
        FieldDef("content", "Areas covered / Notes", multiline = true)
    )

    val eventFields = listOf(
        FieldDef("title", "Event Name", required = true),
        FieldDef("content", "Date / Time / Details", required = true, multiline = true),
        FieldDef("category", "Category", isCategory = true)
    )

    val reservationFields = listOf(
        FieldDef("title", "Title", required = true),
        FieldDef("content", "Reservation Policy / Details", multiline = true)
    )

    val policyFields = listOf(
        FieldDef("title", "Policy Title", required = true),
        FieldDef("content", "Policy Details", required = true, multiline = true)
    )

    val ruleFields = listOf(
        FieldDef("title", "Rule Title", required = true),
        FieldDef("content", "Rule Content (what AI must do)", required = true, multiline = true),
        FieldDef("active", "Active")
    )

    val orderFields = listOf(
        FieldDef("title", "Customer Name", required = true),
        FieldDef("content", "Items Ordered", required = true, multiline = true),
        FieldDef("price", "Total Amount"),
        FieldDef("category", "Order Type (delivery/takeaway/dinein)"),
        FieldDef("phone", "Phone Number")
    )

    fun fieldsFor(type: DataType): List<FieldDef> = when (type) {
        DataType.Menu -> menuFields
        DataType.Deals -> dealFields
        DataType.Services -> serviceFields
        DataType.FAQs -> faqFields
        DataType.DeliveryZones -> deliveryZoneFields
        DataType.Events -> eventFields
        DataType.Reservations -> reservationFields
        DataType.Policies -> policyFields
        DataType.Rules -> ruleFields
        DataType.Orders -> orderFields
    }

    // Map DataType to KB section key
    fun sectionFor(type: DataType): String = when (type) {
        DataType.Menu -> "menu"
        DataType.Deals -> "promotions"
        DataType.Services -> "services"
        DataType.FAQs -> "faqs"
        DataType.DeliveryZones -> "delivery_zones"
        DataType.Events -> "events"
        DataType.Reservations -> "reservations"
        DataType.Policies -> "policies"
        DataType.Rules -> "rules"
        DataType.Orders -> "orders"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDataScreen(
    vm: AppViewModel,
    initialType: DataType = DataType.Menu,
    editingId: String? = null,
    onBack: () -> Unit = {},
    onSaved: () -> Unit
) {
    var selectedType by remember { mutableStateOf(initialType) }
    val scrollState = rememberScrollState()

    // Field values — keyed by field key
    val fields = FieldSchema.fieldsFor(selectedType)
    val fieldValues = remember(selectedType, editingId) {
        mutableStateMapOf<String, String>()
    }
    val boolValues = remember(selectedType, editingId) {
        mutableStateMapOf<String, Boolean>()
    }

    // Load existing item if editing
    LaunchedEffect(editingId, selectedType) {
        if (editingId != null) {
            if (selectedType == DataType.Rules) {
                val rule = vm.rules.value.find { it.id == editingId }
                if (rule != null) {
                    fieldValues["title"] = rule.title
                    fieldValues["content"] = rule.content
                    boolValues["active"] = rule.active
                }
            } else if (selectedType == DataType.Orders) {
                val order = vm.orders.value.find { it.id == editingId }
                if (order != null) {
                    fieldValues["title"] = order.customerName
                    fieldValues["content"] = order.items
                    fieldValues["price"] = order.totalAmount
                    fieldValues["category"] = order.orderType
                    fieldValues["phone"] = order.customerPhone
                }
            } else {
                val item = vm.kbItems.value.find { it.id == editingId }
                if (item != null) {
                    fieldValues["title"] = item.title
                    fieldValues["content"] = item.content
                    fieldValues["description"] = item.description
                    fieldValues["price"] = item.price
                    fieldValues["category"] = item.category
                    boolValues["available"] = item.available
                    boolValues["active"] = item.active
                }
            }
        }
    }

    // Reset fields when type changes (only if not editing)
    LaunchedEffect(selectedType) {
        if (editingId == null) {
            fieldValues.clear()
            boolValues.clear()
        }
    }

    var validationError by remember { mutableStateOf<String?>(null) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var categoryFieldKey by remember { mutableStateOf("category") }

    // Categories for the current section
    val sectionKey = FieldSchema.sectionFor(selectedType)
    val categories = remember(selectedType) { vm.categoriesBySection(sectionKey) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editingId != null) "Edit ${selectedType.label}" else "Add Data") },
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
            .verticalScroll(scrollState)
            .padding(padding)
            .padding(16.dp)
    ) {
        // ── Dynamic Fields ──
        CardBox {
            Text("${selectedType.label} Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))

            fields.forEach { field ->
                if (field.key == "available" || field.key == "active") {
                    // Boolean field — Switch
                    val checked = boolValues[field.key] ?: true
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(field.label, style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = checked, onCheckedChange = { boolValues[field.key] = it })
                    }
                } else if (field.isCategory) {
                    // Category field — tappable selector that opens a bottom sheet
                    val selectedCategory = fieldValues[field.key] ?: ""
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                categoryFieldKey = field.key
                                showCategorySheet = true
                            }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text(field.label) },
                            placeholder = { Text("Tap to select category") },
                            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    // Text field
                    OutlinedTextField(
                        value = fieldValues[field.key] ?: "",
                        onValueChange = { fieldValues[field.key] = it },
                        label = { Text(field.label + if (field.required) " *" else "") },
                        singleLine = !field.multiline,
                        maxLines = if (field.multiline) 5 else 1,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                }
            }
        }

        // ── Validation Error ──
        validationError?.let { err ->
            Spacer(Modifier.height(8.dp))
            Text(err, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }

        Spacer(Modifier.height(16.dp))

        // ── Save Button ──
        Button(
            onClick = {
                // Validate required fields
                val missing = fields.filter { it.required && (fieldValues[it.key] ?: "").isBlank() }
                if (missing.isNotEmpty()) {
                    validationError = "Required fields missing: ${missing.joinToString(", ") { it.label }}"
                    return@Button
                }
                validationError = null

                // Save based on type
                when (selectedType) {
                    DataType.Rules -> {
                        val rule = Rule(
                            id = editingId ?: java.util.UUID.randomUUID().toString(),
                            title = fieldValues["title"] ?: "",
                            content = fieldValues["content"] ?: "",
                            active = boolValues["active"] ?: true
                        )
                        if (editingId != null) vm.updateRule(rule) else vm.addRule(rule)
                    }
                    DataType.Orders -> {
                        val order = com.example.whabotpro.data.model.Order(
                            id = editingId ?: java.util.UUID.randomUUID().toString(),
                            customerName = fieldValues["title"] ?: "",
                            items = fieldValues["content"] ?: "",
                            totalAmount = fieldValues["price"] ?: "",
                            orderType = fieldValues["category"] ?: "delivery",
                            customerPhone = fieldValues["phone"] ?: ""
                        )
                        if (editingId != null) vm.updateOrder(order) else vm.addOrder(order)
                    }
                    else -> {
                        val section = FieldSchema.sectionFor(selectedType)
                        val item = KbItem(
                            id = editingId ?: java.util.UUID.randomUUID().toString(),
                            section = section,
                            title = fieldValues["title"] ?: "",
                            content = fieldValues["content"] ?: "",
                            description = fieldValues["description"] ?: "",
                            price = fieldValues["price"] ?: "",
                            category = fieldValues["category"] ?: "",
                            available = boolValues["available"] ?: true,
                            active = boolValues["active"] ?: true,
                            updatedAt = System.currentTimeMillis()
                        )
                        if (editingId != null) vm.updateKbItem(item) else vm.addKbItem(item)
                    }
                }
                onSaved()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Save, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (editingId != null) "Update" else "Save")
        }
    }
    } // end Scaffold

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
                            "No categories yet for ${selectedType.label}.",
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
                                        section = sectionKey,
                                        name = newCatName.trim()
                                    )
                                    vm.addCategory(cat)
                                    fieldValues[categoryFieldKey] = newCatName.trim()
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
                                if (fieldValues[categoryFieldKey] == cat.name) {
                                    Icon(Icons.Filled.Check, contentDescription = "Selected")
                                }
                            },
                            modifier = Modifier.clickable {
                                fieldValues[categoryFieldKey] = cat.name
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
                                                section = sectionKey,
                                                name = newCatName.trim()
                                            )
                                            vm.addCategory(cat)
                                            fieldValues[categoryFieldKey] = newCatName.trim()
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
