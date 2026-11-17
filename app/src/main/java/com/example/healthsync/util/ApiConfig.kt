package com.example.healthsync.util

/**
 * Central configuration for API endpoints and base URL.
 * Developers can modify these values to point to their own server.
 */
object ApiConfig {
    // Change this to your server's base URL
    const val DEFAULT_BASE_URL = "https://api.teb-data.ir"

    // Ingest endpoint path
    const val DEFAULT_INGEST_PATH = "api/health-syncs"

    // Auth endpoints (prefixed with the API path)
    const val AUTH_SEND_CODE = "api/auth/send-verification-code"
    const val AUTH_LOGIN = "api/auth/register-or-login"

    // Health record endpoints
    const val HEALTH_RECORDS = "api/health-records"
    const val HEALTH_RECORDS_METRIC_TYPES = "api/health-records/metric-types"
    const val HEALTH_RECORDS_METRIC_COMPONENTS = "api/health-records/metric-components"
    const val HEALTH_RECORDS_BY_USER = "api/health-records/by-user"
    const val HEALTH_INSIGHTS_LAST = "api/health-insights/last"

    // Medical record endpoints
    const val MEDICAL_RECORDS = "api/medical-records"
    const val MEDICAL_RECORDS_BY_USER = "api/medical-records/by-user"
    const val FILES_INTERPRET_UPLOAD = "api/files/interpret-upload"
    const val FILES_INTERPRETATION = "api/files"
}
