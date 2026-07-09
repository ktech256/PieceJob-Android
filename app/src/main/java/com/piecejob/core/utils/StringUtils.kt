package com.piecejob.core.utils

fun formatPrivacyAddress(address: String?): String {
    if (address == null) return "Nearby Location"
    val parts = address.split(",")
    return if (parts.size >= 3) {
        // Example: "139 Erasmus St, Flora Park, Polokwane" -> "Flora Park, Polokwane"
        "${parts[1].trim()}, ${parts[2].trim()}"
    } else if (parts.isNotEmpty()) {
        address.trim()
    } else {
        "Nearby Location"
    }
}

fun formatDateTimeString(isoString: String?): String {
    if (isoString == null) return "N/A"
    return try {
        // Basic extraction for "08 Jul 2026 • 14:32" style or just a simple split
        // ISO: 2024-03-20T10:15:00.000Z
        val datePart = isoString.take(10)
        val timePart = isoString.substring(11, 16)
        "$datePart • $timePart"
    } catch (e: Exception) {
        isoString.take(16).replace("T", " ")
    }
}
