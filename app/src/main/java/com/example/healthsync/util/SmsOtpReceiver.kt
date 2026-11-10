package com.example.healthsync.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat

/**
 * BroadcastReceiver for Google Play Services SMS User Consent API.
 * Listens for "com.google.android.gms.auth.api.phone.SMS_RETRIEVED" action.
 *
 * Does NOT require android.permission.RECEIVE_SMS permission.
 */
class SmsOtpReceiver(
    private val onNeedUserConsent: (Intent) -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return

        if (intent.action == ACTION_SMS_RETRIEVED) {
            val extras = intent.extras ?: return

            val statusObj = extras.get(EXTRA_STATUS)
            val statusCode = getStatusCode(statusObj)

            if (statusCode == 0) { // 0 == CommonStatusCodes.SUCCESS
                val consentIntent = extras.get(EXTRA_CONSENT_INTENT) as? Intent
                if (consentIntent != null) {
                    onNeedUserConsent(consentIntent)
                }
            }
        }
    }

    private fun getStatusCode(statusObj: Any?): Int? {
        if (statusObj == null) return null
        return try {
            val method = statusObj.javaClass.getMethod("getStatusCode")
            method.invoke(statusObj) as? Int
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        const val ACTION_SMS_RETRIEVED = "com.google.android.gms.auth.api.phone.SMS_RETRIEVED"
        const val EXTRA_STATUS = "com.google.android.gms.auth.api.phone.EXTRA_STATUS"
        const val EXTRA_CONSENT_INTENT = "com.google.android.gms.auth.api.phone.EXTRA_CONSENT_INTENT"
        const val EXTRA_SMS_MESSAGE = "com.google.android.gms.auth.api.phone.EXTRA_SMS_MESSAGE"

        fun register(
            context: Context,
            onNeedUserConsent: (Intent) -> Unit
        ): SmsOtpReceiver {
            val receiver = SmsOtpReceiver(onNeedUserConsent)
            val intentFilter = IntentFilter(ACTION_SMS_RETRIEVED)
            ContextCompat.registerReceiver(
                context,
                receiver,
                intentFilter,
                ContextCompat.RECEIVER_EXPORTED
            )
            return receiver
        }

        fun unregister(context: Context, receiver: SmsOtpReceiver?) {
            if (receiver != null) {
                try {
                    context.unregisterReceiver(receiver)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        /**
         * Starts SMS User Consent listener using Google Play Services SmsRetriever client.
         * Accepts null to listen for SMS from any sender.
         */
        fun startSmsUserConsent(context: Context, senderPhoneNumber: String? = null) {
            try {
                val smsRetrieverClass = Class.forName("com.google.android.gms.auth.api.phone.SmsRetriever")
                val getClientMethod = smsRetrieverClass.getMethod("getClient", Context::class.java)
                val client = getClientMethod.invoke(null, context)
                val startConsentMethod = client.javaClass.getMethod("startSmsUserConsent", String::class.java)
                startConsentMethod.invoke(client, senderPhoneNumber)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
