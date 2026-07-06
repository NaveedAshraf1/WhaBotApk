package com.example.whabotpro.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

/**
 * Reads the phone numbers stored on the device's SIM/eSIM slots using the modern
 * TelephonyManager APIs.
 *
 * Required permission:
 *   - Android 10+ (API 29+): READ_PHONE_NUMBERS
 *   - Android 9 and below: READ_PHONE_STATE
 *
 * Numbers are normalised so they always start with the +country-code prefix.
 */
object SimNumberHelper {

    data class SimLine(
        val slotIndex: Int,
        val carrierName: String,
        val rawNumber: String,
        val formattedNumber: String
    )

    fun getRequiredPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Manifest.permission.READ_PHONE_NUMBERS
        } else {
            Manifest.permission.READ_PHONE_STATE
        }
    }

    fun hasPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, getRequiredPermission()) ==
                PackageManager.PERMISSION_GRANTED
    }

    fun getSimNumbers(context: Context): List<SimLine> {
        if (!hasPermission(context)) return emptyList()

        val results = mutableListOf<SimLine>()
        val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            ?: return emptyList()

        val baseTelephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val defaultCountryIso = baseTelephonyManager?.networkCountryIso?.uppercase()
            ?: baseTelephonyManager?.simCountryIso?.uppercase()
            ?: java.util.Locale.getDefault().country

        try {
            val subscriptions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                subscriptionManager.getActiveSubscriptionInfoList()
            } else {
                @Suppress("DEPRECATION")
                subscriptionManager.activeSubscriptionInfoList
            }

            subscriptions?.forEach { info ->
                val subId = info.subscriptionId
                val slotIndex = info.simSlotIndex
                val carrierName = info.carrierName?.toString() ?: "SIM ${slotIndex + 1}"

                // Try the modern per-subscription API first
                val rawNumber = readNumberForSubscription(baseTelephonyManager, subId)
                    ?: @Suppress("DEPRECATION") info.number?.trim()
                    ?: ""

                if (rawNumber.isBlank()) return@forEach

                val formattedNumber = normaliseNumber(rawNumber, defaultCountryIso)
                results.add(
                    SimLine(
                        slotIndex = slotIndex,
                        carrierName = carrierName,
                        rawNumber = rawNumber,
                        formattedNumber = formattedNumber
                    )
                )
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return results.distinctBy { it.formattedNumber }
    }

    private fun readNumberForSubscription(telephonyManager: TelephonyManager?, subId: Int): String? {
        if (telephonyManager == null) return null
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val subManager = telephonyManager.createForSubscriptionId(subId)
                @Suppress("DEPRECATION")
                subManager.line1Number?.trim()
            } else {
                @Suppress("DEPRECATION")
                telephonyManager.line1Number?.trim()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Normalise a number so it always starts with the +country-code prefix.
     */
    private fun normaliseNumber(number: String, countryIso: String?): String {
        var cleaned = number.replace("[\\s\\-()]+".toRegex(), "")

        // Already has a + or international prefix
        if (cleaned.startsWith("+")) return cleaned
        if (cleaned.startsWith("00")) return "+" + cleaned.drop(2)

        val countryCode = countryCodeForIso(countryIso ?: "")
        return if (countryCode != null && !cleaned.startsWith(countryCode)) {
            // Remove leading national trunk prefix (e.g. 0) if present
            if (cleaned.startsWith("0")) {
                cleaned = cleaned.drop(1)
            }
            "+$countryCode$cleaned"
        } else {
            cleaned
        }
    }

    private fun countryCodeForIso(countryIso: String): String? {
        return when (countryIso.uppercase()) {
            "PK" -> "92"
            "US", "CA" -> "1"
            "GB" -> "44"
            "IN" -> "91"
            "DE" -> "49"
            "FR" -> "33"
            "AE" -> "971"
            "SA" -> "966"
            "TR" -> "90"
            "EG" -> "20"
            "BD" -> "880"
            "ID" -> "62"
            "BR" -> "55"
            "MX" -> "52"
            "NG" -> "234"
            "ZA" -> "27"
            "RU" -> "7"
            "CN" -> "86"
            "JP" -> "81"
            "KR" -> "82"
            "TH" -> "66"
            "VN" -> "84"
            "MY" -> "60"
            "PH" -> "63"
            "SG" -> "65"
            "AU" -> "61"
            "NZ" -> "64"
            "IT" -> "39"
            "ES" -> "34"
            "NL" -> "31"
            "BE" -> "32"
            "CH" -> "41"
            "AT" -> "43"
            "SE" -> "46"
            "NO" -> "47"
            "DK" -> "45"
            "FI" -> "358"
            "PL" -> "48"
            "UA" -> "380"
            "RO" -> "40"
            "CZ" -> "420"
            "HU" -> "36"
            "GR" -> "30"
            "PT" -> "351"
            "IE" -> "353"
            "IL" -> "972"
            "QA" -> "974"
            "KW" -> "965"
            "BH" -> "973"
            "OM" -> "968"
            "JO" -> "962"
            "LB" -> "961"
            "IQ" -> "964"
            "IR" -> "98"
            "AF" -> "93"
            "LK" -> "94"
            "NP" -> "977"
            "MM" -> "95"
            "KH" -> "855"
            "LA" -> "856"
            else -> null
        }
    }
}
