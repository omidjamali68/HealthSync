package com.example.healthsync.util

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject

object JwtUtils {

    fun isAdmin(token: String?): Boolean {
        if (token.isNullOrBlank()) return false
        val parts = token.split(".")
        if (parts.size < 2) return false

        return try {
            val jsonString = decodeBase64(parts[1]) ?: return false
            val jsonObject = JSONObject(jsonString)
            val roles = extractRoles(jsonObject)
            roles.any { it.equals("admin", ignoreCase = true) }
        } catch (e: Exception) {
            false
        }
    }

    private fun decodeBase64(base64Str: String): String? {
        return try {
            val flags = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            val decodedBytes = Base64.decode(base64Str, flags)
            String(decodedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            try {
                val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
                String(decodedBytes, Charsets.UTF_8)
            } catch (e2: Exception) {
                null
            }
        }
    }

    private fun extractRoles(jsonObject: JSONObject): List<String> {
        val roles = mutableListOf<String>()

        val roleKeys = listOf(
            "role",
            "roles",
            "http://schemas.microsoft.com/ws/2008/06/identity/claims/role",
            "user_roles",
            "userRoles"
        )

        for (key in roleKeys) {
            if (jsonObject.has(key)) {
                when (val value = jsonObject.opt(key)) {
                    is JSONArray -> {
                        for (i in 0 until value.length()) {
                            val role = value.optString(i)
                            if (role.isNotBlank()) roles.add(role)
                        }
                    }
                    is String -> {
                        if (value.isNotBlank()) roles.add(value)
                    }
                }
            }
        }

        // Keycloak realm_access
        if (jsonObject.has("realm_access")) {
            val realmAccess = jsonObject.optJSONObject("realm_access")
            val realmRoles = realmAccess?.optJSONArray("roles")
            if (realmRoles != null) {
                for (i in 0 until realmRoles.length()) {
                    val role = realmRoles.optString(i)
                    if (role.isNotBlank()) roles.add(role)
                }
            }
        }

        return roles
    }
}
