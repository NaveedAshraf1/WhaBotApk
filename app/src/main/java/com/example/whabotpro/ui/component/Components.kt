package com.example.whabotpro.ui.component

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whabotpro.data.model.WaState

@Composable
fun StatusPill(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f),
        modifier = Modifier.padding(end = 6.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun WaStatusPill(state: WaState) {
    val (text, color) = when (state) {
        WaState.CONNECTED -> "WhatsApp: Connected" to Color(0xFF25D366)
        WaState.QR_READY -> "WhatsApp: QR Ready" to Color(0xFFFFA500)
        WaState.CODE_READY -> "WhatsApp: Code Ready" to Color(0xFFFFA500)
        WaState.CONNECTING -> "WhatsApp: Connecting..." to Color(0xFF8696A0)
        WaState.ERROR -> "WhatsApp: Error" to Color(0xFFFF4444)
        WaState.DISCONNECTED -> "WhatsApp: Off" to Color(0xFF8696A0)
    }
    StatusPill(text, color)
}

@Composable
fun QrCodeImage(dataUrl: String?, modifier: Modifier = Modifier) {
    if (dataUrl == null) {
        Box(
            modifier = modifier
                .size(280.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    val bitmap = remember(dataUrl) {
        try {
            val base64 = dataUrl.substringAfter("base64,")
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) { null }
    }
    if (bitmap != null) {
        // Use ContentFit.FillWidth so the QR is never cropped or stretched
        Surface(
            modifier = modifier
                .size(280.dp)
                .clip(RoundedCornerShape(16.dp)),
            color = Color.White
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "QR Code",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    } else {
        Box(
            modifier = modifier
                .size(280.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("QR decode error", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String = "") {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        if (subtitle.isNotEmpty()) {
            Text(subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
    }
}

@Composable
fun CardBox(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun LogoMark(text: String = "W", size: Int = 40) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
    }
}
