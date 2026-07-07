package com.example.whabotpro.ui.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class Screen(
    val title: String,
    val icon: ImageVector,
    val route: String
) {
    Dashboard("Dashboard", Icons.Filled.Dashboard, "dashboard"),
    RawData("Add Raw Data", Icons.Filled.AutoFixHigh, "raw_data"),
    Connect("Connect WhatsApp", Icons.Filled.QrCodeScanner, "connect"),
    BulkSend("Bulk Send", Icons.Filled.Group, "bulk_send"),
    Inbox("Inbox", Icons.Filled.Inbox, "inbox"),
    Business("My Business", Icons.Filled.Store, "business"),
    Menu("Menu", Icons.Filled.RestaurantMenu, "menu"),
    Orders("Orders", Icons.Filled.ShoppingCart, "orders"),
    Deals("Deals", Icons.Filled.LocalOffer, "promotions"),
    DeliveryZones("Delivery Zones", Icons.Filled.LocalShipping, "delivery_zones"),
    Services("Services", Icons.Filled.RoomService, "services"),
    Events("Events", Icons.Filled.Event, "events"),
    Reservations("Reservations", Icons.Filled.TableRestaurant, "reservations"),
    FAQs("FAQs", Icons.Filled.Help, "faqs"),
    Policies("Policies", Icons.Filled.Policy, "policies"),
    Contacts("Contacts", Icons.Filled.Contacts, "contacts"),
    Rules("Rules", Icons.Filled.Rule, "rules"),
    TestChat("Test Chat", Icons.Filled.Science, "test_chat"),
    Logs("Logs", Icons.Filled.Terminal, "logs"),
    Settings("Settings", Icons.Filled.Settings, "settings");

    companion object {
        val kbSections = mapOf(
            "menu" to Menu,
            "services" to Services,
            "faqs" to FAQs,
            "promotions" to Deals,
            "delivery_zones" to DeliveryZones,
            "events" to Events,
            "reservations" to Reservations,
            "policies" to Policies
        )
    }
}
