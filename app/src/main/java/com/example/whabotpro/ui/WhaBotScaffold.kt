package com.example.whabotpro.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.whabotpro.ui.component.LogoMark
import com.example.whabotpro.ui.component.WaStatusPill
import com.example.whabotpro.ui.screen.*
import com.example.whabotpro.ui.viewmodel.AppViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhaBotApp(context: android.content.Context) {
    val vm: AppViewModel = viewModel()
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val currentScreen = Screen.entries.find { it.route == currentRoute } ?: Screen.Dashboard
    val waState by vm.waState.collectAsState()
    val settings by vm.settings.collectAsState()

    // Important screens shown in the BottomAppBar.
    // Only the selected item shows its label; the others show just their icon.
    val bottomNavScreens = remember {
        listOf(Screen.Dashboard, Screen.Menu, Screen.Orders, Screen.Deals, Screen.Reservations, Screen.Services)
    }

    // Screens that have an "add data" action — show a plus icon in the toolbar for these.
    // WhatsApp status pill is only shown on Dashboard.
    val addableScreens = remember {
        setOf(
            Screen.Menu, Screen.Deals, Screen.DeliveryZones, Screen.Services,
            Screen.Events, Screen.Reservations, Screen.FAQs, Screen.Policies,
            Screen.Rules, Screen.Orders
        )
    }
    var addTrigger by remember { mutableIntStateOf(0) }

    val navTo: (Screen) -> Unit = { screen ->
        navController.navigate(screen.route) { launchSingleTop = true }
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    LogoMark()
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("WhaBotPro", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(settings.businessName, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Divider()
                // Nav items — exclude screens already in the bottom nav + Connect + BulkSend
                Screen.entries.filter { it !in bottomNavScreens && it != Screen.Connect && it != Screen.BulkSend }.forEach { screen ->
                    NavigationDrawerItem(
                        label = { Text(screen.title) },
                        selected = currentScreen == screen,
                        icon = { Icon(screen.icon, contentDescription = null) },
                        onClick = { navTo(screen) },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(currentScreen.title) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        if (currentScreen == Screen.Dashboard) {
                            WaStatusPill(waState)
                            Spacer(Modifier.width(8.dp))
                        } else if (currentScreen in addableScreens) {
                            IconButton(onClick = { addTrigger++ }) {
                                Icon(Icons.Filled.Add, contentDescription = "Add")
                            }
                            Spacer(Modifier.width(4.dp))
                        }
                    }
                )
            },
            bottomBar = {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 3.dp
                ) {
                    bottomNavScreens.forEach { screen ->
                        val selected = currentScreen == screen
                        val tint = if (selected) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clickable { navTo(screen) }
                        ) {
                            Icon(screen.icon, contentDescription = screen.title, tint = tint)
                            AnimatedVisibility(
                                visible = selected,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Text(
                                    screen.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = tint,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.padding(padding)
            ) {
                composable(Screen.Dashboard.route) { DashboardScreen(vm, navTo) }
                composable(Screen.RawData.route) { AddRawDataScreen(vm, onBack = { navTo(Screen.Dashboard) }) }
                composable(Screen.Connect.route) { ConnectScreen(vm) }
                composable(Screen.BulkSend.route) { BulkSendScreen(vm) }
                composable(Screen.Inbox.route) { InboxScreen(vm) }
                composable(Screen.Business.route) { BusinessScreen(vm) }
                composable(Screen.Menu.route) { KbSectionScreen(vm, "menu", "Menu", "Manage your menu items and categories", addTrigger) }
                composable(Screen.Orders.route) { OrdersScreen(vm, addTrigger) }
                composable(Screen.Deals.route) { KbSectionScreen(vm, "promotions", "Deals", "Active deals, discounts, and special offers", addTrigger) }
                composable(Screen.DeliveryZones.route) { KbSectionScreen(vm, "delivery_zones", "Delivery Zones", "Areas you deliver to, charges, and times", addTrigger) }
                composable(Screen.Services.route) { KbSectionScreen(vm, "services", "Services", "Services offered by your business", addTrigger) }
                composable(Screen.Events.route) { KbSectionScreen(vm, "events", "Events", "Special events and happenings", addTrigger) }
                composable(Screen.Reservations.route) { KbSectionScreen(vm, "reservations", "Reservations", "Reservation policies and info", addTrigger) }
                composable(Screen.FAQs.route) { KbSectionScreen(vm, "faqs", "FAQs", "Frequently asked questions", addTrigger) }
                composable(Screen.Policies.route) { KbSectionScreen(vm, "policies", "Policies", "Business policies", addTrigger) }
                composable(Screen.Rules.route) { RulesScreen(vm, addTrigger) }
                composable(Screen.TestChat.route) { TestChatScreen(vm) }
                composable(Screen.Logs.route) { LogsScreen(vm) }
                composable(Screen.Settings.route) { SettingsScreen(vm, context) }
            }
        }
    }
}
