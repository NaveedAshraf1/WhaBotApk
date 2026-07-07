package com.example.whabotpro.ui.screen

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.whabotpro.data.model.Order
import com.example.whabotpro.ui.component.*
import com.example.whabotpro.ui.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun OrdersScreen(vm: AppViewModel, addTrigger: Int = 0) {
    val orders by vm.orders.collectAsState()
    var showEditor by remember { mutableStateOf(false) }
    var statusFilter by remember { mutableStateOf("") }
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    val filtered = if (statusFilter.isEmpty()) orders else orders.filter { it.status == statusFilter }

    // React to toolbar + button
    LaunchedEffect(addTrigger) {
        if (addTrigger > 0) showEditor = true
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            val statuses = listOf("", "pending", "preparing", "ready", "delivered", "cancelled")
            statuses.forEach { s ->
                FilterChip(
                    selected = statusFilter == s,
                    onClick = { statusFilter = s },
                    label = { Text(if (s.isEmpty()) "All" else s.replaceFirstChar { it.uppercase() }) },
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }

        if (filtered.isEmpty()) {
            EmptyState("No orders found.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered) { order -> OrderCard(order, dateFormat, vm) }
            }
        }
    }

    if (showEditor) {
        OrderEditorDialog(onDismiss = { showEditor = false }, onSave = { vm.addOrder(it); showEditor = false })
    }
}

@Composable
private fun OrderCard(order: Order, dateFormat: SimpleDateFormat, vm: AppViewModel) {
    val statusColor = when (order.status) {
        "pending" -> Color(0xFFFFA500)
        "preparing" -> Color(0xFF2196F3)
        "ready" -> Color(0xFF25D366)
        "delivered" -> Color(0xFF8696A0)
        "cancelled" -> Color(0xFFFF4444)
        else -> Color(0xFF8696A0)
    }
    CardBox {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(order.orderNumber, style = MaterialTheme.typography.titleMedium)
                Text("Customer: ${order.customerName} (${order.customerPhone})", style = MaterialTheme.typography.bodySmall)
                Text("Items: ${order.items}", style = MaterialTheme.typography.bodyMedium)
                if (order.totalAmount.isNotEmpty()) Text("Total: ${order.totalAmount}", style = MaterialTheme.typography.bodySmall)
                Text("Type: ${order.orderType}  |  ${dateFormat.format(Date(order.createdAt))}", style = MaterialTheme.typography.labelSmall)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("Status: ", style = MaterialTheme.typography.labelSmall)
                    Text(order.status, style = MaterialTheme.typography.labelSmall, color = statusColor)
                }
            }
            Column {
                IconButton(onClick = {
                    val next = when (order.status) {
                        "pending" -> "preparing"
                        "preparing" -> "ready"
                        "ready" -> "delivered"
                        else -> order.status
                    }
                    vm.updateOrder(order.copy(status = next))
                }) { Icon(Icons.Filled.ArrowForward, "Advance") }
                IconButton(onClick = { vm.deleteOrder(order.id) }) {
                    Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun OrderEditorDialog(onDismiss: () -> Unit, onSave: (Order) -> Unit) {
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var items by remember { mutableStateOf("") }
    var totalAmount by remember { mutableStateOf("") }
    var orderType by remember { mutableStateOf("delivery") }
    var address by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Order") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = customerName, onValueChange = { customerName = it }, label = { Text("Customer Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = customerPhone, onValueChange = { customerPhone = it }, label = { Text("Customer Phone") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = items, onValueChange = { items = it }, label = { Text("Items") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = totalAmount, onValueChange = { totalAmount = it }, label = { Text("Total Amount") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = orderType, onValueChange = { orderType = it }, label = { Text("Order Type (delivery/takeaway/dinein)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(Order(
                    orderNumber = "ORD-${System.currentTimeMillis().toString().takeLast(8)}-${(1000..9999).random()}",
                    customerName = customerName, customerPhone = customerPhone, items = items,
                    totalAmount = totalAmount, orderType = orderType, address = address, status = "pending"
                ))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
