package com.example.healthsync.util

/**
 * Central configuration for API endpoints and base URL.
 * Developers can modify these values to point to their own server.
 */
object ApiConfig {
    // Change this to your server's base URL
    const val DEFAULT_BASE_URL = "http://10.0.2.2:5000"

    // Ingest endpoint path
    const val DEFAULT_INGEST_PATH = "api/health-syncs"

    // Auth endpoints (prefixed with the API path)
    const val AUTH_SEND_CODE = "api/auth/send-verification-code"
    const val AUTH_LOGIN = "api/auth/register-or-login"
}
