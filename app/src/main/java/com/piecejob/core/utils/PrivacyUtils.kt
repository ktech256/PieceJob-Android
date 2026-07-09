package com.piecejob.core.utils

object PrivacyUtils {
    /**
     * Formats an address to obscure precise details (house number, street) 
     * while showing the suburb and city.
     * Example: "139 Erasmus St, Flora Park, Polokwane" -> "Flora Park, Polokwane"
     */
    fun obscureAddress(address: String?): String {
        if (address == null || address.isBlank()) return "Nearby Location"
        
        val parts = address.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        
        return when {
            parts.size >= 3 -> {
                // If it's a standard address, the first part is usually "Number Street"
                // We take the 2nd and 3rd parts (Suburb and City)
                "${parts[1]}, ${parts[2]}"
            }
            parts.size == 2 -> {
                // If only 2 parts, we keep them but check if the first one looks like a street
                if (hasNumbers(parts[0])) {
                    parts[1] // Return only the 2nd part (likely City)
                } else {
                    "${parts[0]}, ${parts[1]}"
                }
            }
            else -> {
                // Single part, check if it's a street or suburb/city
                if (hasNumbers(parts[0])) "Nearby Location" else parts[0]
            }
        }
    }

    private fun hasNumbers(text: String): Boolean {
        return text.any { it.isDigit() }
    }
}
