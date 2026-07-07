package com.example.whabotpro.ui.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class Screen(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String
) {
    Dashboard("Dashboard", "Overview of your WhatsApp AI bot", Icons.Filled.Dashboard, "dashboard"),
    RawData("Add Raw Data", "Paste raw data and let AI parse it into the right tables", Icons.Filled.AutoFixHigh, "raw_data"),
    Connect("Connect WhatsApp", "Link via QR scan or phone number pairing code", Icons.Filled.QrCodeScanner, "connect"),
    BulkSend("Bulk Send", "Send the same message to multiple numbers (one per line)", Icons.Filled.Group, "bulk_send"),
    Inbox("Inbox", "Recent incoming and outgoing messages", Icons.Filled.Inbox, "inbox"),
    Business("My Business", "Brand, contact, and operating details the AI uses in replies", Icons.Filled.Store, "business"),
    Menu("Menu", "Manage your menu items and categories", Icons.Filled.RestaurantMenu, "menu"),
    Orders("Orders", "Manage delivery, takeaway, and dine-in orders", Icons.Filled.ShoppingCart, "orders"),
    Deals("Deals", "Active deals, discounts, and special offers", Icons.Filled.LocalOffer, "promotions"),
    DeliveryZones("Delivery Zones", "Areas you deliver to, charges, and times", Icons.Filled.LocalShipping, "delivery_zones"),
    Services("Services", "Services offered by your business", Icons.Filled.RoomService, "services"),
    Events("Events", "Special events and happenings", Icons.Filled.Event, "events"),
    Reservations("Reservations", "Reservation policies and info", Icons.Filled.TableRestaurant, "reservations"),
    FAQs("FAQs", "Frequently asked questions", Icons.Filled.Help, "faqs"),
    Policies("Policies", "Store policies (refund, cancellation, etc.)", Icons.Filled.Policy, "policies"),
    Contacts("Contacts", "People who have messaged your WhatsApp", Icons.Filled.Contacts, "contacts"),
    Rules("Rules", "Custom rules the AI must follow when replying", Icons.Filled.Rule, "rules"),
    TestChat("Test Chat", "Test the AI agent without sending real WhatsApp messages", Icons.Filled.Science, "test_chat"),
    Logs("Logs", "System and debug logs", Icons.Filled.Terminal, "logs"),
    Settings("Settings", "Configure AI, server, and bot behavior", Icons.Filled.Settings, "settings");

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
