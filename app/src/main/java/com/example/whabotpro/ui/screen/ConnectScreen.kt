package com.example.whabotpro.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.core.content.FileProvider
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whabotpro.data.model.WaState
import com.example.whabotpro.ui.component.*
import com.example.whabotpro.ui.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream

private fun copyToClipboard(context: Context, text: String, label: String = "Pairing code") {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
}

private fun shareQrCode(context: Context, qrCode: String?) {
    val dataUrl = qrCode ?: return
    try {
        val base64 = dataUrl.substringAfter("base64,")
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        if (original != null) {
            // Add white padding around the QR code so it's not cut when shared
            val padding = (original.width * 0.15f).toInt().coerceAtLeast(40)
            val padded = Bitmap.createBitmap(
                original.width + padding * 2,
                original.height + padding * 2,
                Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(padded)
            canvas.drawColor(android.graphics.Color.WHITE)
            canvas.drawBitmap(original, padding.toFloat(), padding.toFloat(), null)

            val file = File(context.cacheDir, "whatsapp_qr.png")
            FileOutputStream(file).use { padded.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Scan this QR code to connect WhatsApp")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share QR Code"))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectScreen(vm: AppViewModel) {
    val waState by vm.waState.collectAsState()
    val qrCode by vm.qrCode.collectAsState()
    val connectedUser by vm.connectedUser.collectAsState()
    val isRequestingPairing by vm.isRequestingPairing.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var pairingMode by remember { mutableStateOf(true) }
        var phoneNumber by remember { mutableStateOf("") }
        val pairingCode by vm.pairingCode.collectAsState()

        Spacer(Modifier.height(24.dp))

        // Mode toggle (only when not connected)
        if (waState != WaState.CONNECTED) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                SegmentedButton(
                    selected = !pairingMode,
                    onClick = { pairingMode = false; vm.refreshQr() },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("QR Code") }
                SegmentedButton(
                    selected = pairingMode,
                    onClick = { pairingMode = true; vm.logout() },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("Phone Number") }
            }
            Spacer(Modifier.height(24.dp))
        }

        // Phone number input — always rendered in a STABLE position when in pairing mode
        // and not yet connected / not showing the pairing code.
        // This prevents focus loss caused by the composable moving between when() branches.
        if (waState != WaState.CONNECTED && pairingMode && waState != WaState.CODE_READY) {
            PhoneNumberInputCard(
                phoneNumber = phoneNumber,
                onPhoneChange = { phoneNumber = it },
                onRequestCode = { vm.startPairingCode(phoneNumber) },
                isLoading = isRequestingPairing
            )
            Spacer(Modifier.height(16.dp))
        }

        when (waState) {
            WaState.CONNECTED -> {
                // Connected state
                CardBox {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("WhatsApp Connected!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        if (connectedUser.isNotEmpty()) {
                            Text(connectedUser, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(24.dp))
                        OutlinedButton(onClick = { vm.logout() }) {
                            Text("Disconnect / Re-scan")
                        }
                    }
                }
            }
            WaState.QR_READY -> {
                if (!pairingMode) {
                    // Countdown timer — WhatsApp QR expires in ~20 seconds
                    var secondsLeft by remember(qrCode) { mutableIntStateOf(20) }
                    var isExpired by remember(qrCode) { mutableStateOf(false) }

                    LaunchedEffect(qrCode) {
                        secondsLeft = 20
                        isExpired = false
                        while (secondsLeft > 0) {
                            delay(1000)
                            secondsLeft--
                        }
                        isExpired = true
                    }

                    // Auto-refresh when expired
                    LaunchedEffect(isExpired) {
                        if (isExpired) {
                            delay(500)
                            vm.refreshQr()
                        }
                    }

                    // Show QR code
                    Text("Scan to connect WhatsApp", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Open WhatsApp → Settings → Linked Devices → Link a Device",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))

                    // QR card with border and shadow for better scanning
                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                        tonalElevation = 4.dp,
                        shadowElevation = 8.dp,
                        color = Color.White
                    ) {
                        QrCodeImage(dataUrl = qrCode, modifier = Modifier.padding(8.dp))
                    }

                    Spacer(Modifier.height(16.dp))

                    // Countdown timer bar
                    val timerColor = when {
                        secondsLeft > 10 -> Color(0xFF25D366)  // green
                        secondsLeft > 5 -> Color(0xFFFFA000)   // amber
                        else -> MaterialTheme.colorScheme.error // red
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isExpired) {
                            // Expired state
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "QR expired — refreshing...",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            // Active countdown
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "QR valid for ",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "${secondsLeft}s",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = timerColor
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            // Progress bar
                            LinearProgressIndicator(
                                progress = { secondsLeft / 20f },
                                modifier = Modifier
                                    .fillMaxWidth(0.6f)
                                    .height(6.dp)
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp)),
                                color = timerColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = { vm.refreshQr() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Refresh QR")
                        }
                        IconButton(onClick = { shareQrCode(context, qrCode) }) {
                            Icon(Icons.Filled.Share, "Share QR", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            WaState.CODE_READY -> {
                CardBox {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("Enter this pairing code", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Open WhatsApp → Settings → Linked Devices → Link with phone number",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            tonalElevation = 4.dp,
                            shadowElevation = 8.dp,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                pairingCode ?: "----",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 4.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { vm.refreshPairingCode(phoneNumber) }) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Refresh Code")
                            }
                            OutlinedButton(onClick = {
                                pairingCode?.let { copyToClipboard(context, it) }
                            }) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Copy Code")
                            }
                        }
                    }
                }
            }
            WaState.CONNECTING -> {
                if (!pairingMode) {
                    CardBox {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(16.dp))
                            Text("Starting WhatsApp...", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Text("Please wait a moment", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            else -> {
                if (!pairingMode) {
                    CardBox {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text("WhatsApp is not running", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Start the foreground service to connect WhatsApp.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

    }
}

@Composable
private fun PhoneNumberInputCard(
    phoneNumber: String,
    onPhoneChange: (String) -> Unit,
    onRequestCode: () -> Unit,
    isLoading: Boolean = false
) {
    CardBox {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("Connect with Phone Number", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Enter your WhatsApp number to get an 8-digit pairing code.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = onPhoneChange,
                label = { Text("Phone number (with country code)") },
                placeholder = { Text("923001234567") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.85f)
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onRequestCode,
                enabled = phoneNumber.length >= 10 && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("Get Pairing Code")
            }
        }
    }
}
