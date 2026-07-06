package com.example.whabotpro.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.whabotpro.data.model.BusinessInfo
import com.example.whabotpro.ui.component.CardBox
import com.example.whabotpro.ui.component.SectionHeader
import com.example.whabotpro.ui.viewmodel.AppViewModel

@Composable
fun BusinessScreen(vm: AppViewModel) {
    val info by vm.businessInfo.collectAsState()
    val fields = remember(info) { mutableStateOf(info) }

    var saved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        SectionHeader("My Business", "Brand, contact, and operating details the AI uses in replies")

        CardBox {
            val i = fields.value
            BizField("Brand Name", i.brandName) { fields.value = i.copy(brandName = it) }
            BizField("Tagline", i.tagline) { fields.value = i.copy(tagline = it) }
            BizField("Category", i.category) { fields.value = i.copy(category = it) }
            BizField("Phone", i.phone) { fields.value = i.copy(phone = it) }
            BizField("Email", i.email) { fields.value = i.copy(email = it) }
            BizField("Website", i.website) { fields.value = i.copy(website = it) }
            BizField("Address", i.address) { fields.value = i.copy(address = it) }
            BizField("Cuisine", i.cuisine) { fields.value = i.copy(cuisine = it) }
            BizField("Opening Hours", i.openingHours) { fields.value = i.copy(openingHours = it) }
            BizField("Description", i.businessDescription, 3) { fields.value = i.copy(businessDescription = it) }
            BizField("Mission", i.mission) { fields.value = i.copy(mission = it) }
            BizField("Vision", i.vision) { fields.value = i.copy(vision = it) }
            BizField("Core Values", i.coreValues) { fields.value = i.copy(coreValues = it) }
            BizField("Brand Voice", i.brandVoice) { fields.value = i.copy(brandVoice = it) }
            BizField("Unique Selling Proposition", i.uniqueSellingProposition) { fields.value = i.copy(uniqueSellingProposition = it) }
            BizField("Landmarks", i.landmarks) { fields.value = i.copy(landmarks = it) }
            BizField("Extra Notes", i.content, 3) { fields.value = i.copy(content = it) }

            Spacer(Modifier.height(16.dp))
            Button(onClick = { vm.saveBusiness(fields.value); saved = true }) {
                Text("Save Business")
            }
            if (saved) {
                Spacer(Modifier.height(8.dp))
                Text("Saved!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun BizField(label: String, value: String, lines: Int = 1, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = lines == 1,
        maxLines = lines,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}
