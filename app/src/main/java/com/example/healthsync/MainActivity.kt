package com.example.healthsync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.os.LocaleListCompat
import androidx.navigation.compose.rememberNavController
import com.example.healthsync.ui.NavGraph
import com.example.healthsync.ui.Routes
import com.example.healthsync.ui.theme.HealthSyncTheme
import com.example.healthsync.util.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // Force Persian on first run if no preference is saved
        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        if (!prefs.contains("locale_set")) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("fa"))
            prefs.edit().putBoolean("locale_set", true).apply()
        }
        
        super.onCreate(savedInstanceState)

        setContent {
            // Determine the layout direction based on the current app locale
            val currentLocale = AppCompatDelegate.getApplicationLocales()[0] ?: Locale.getDefault()
            val isRtl = currentLocale.language == "fa"
            val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                HealthSyncTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val navController = rememberNavController()
                        val startDestination = if (sessionManager.isLoggedIn()) {
                            Routes.DASHBOARD
                        } else {
                            Routes.AUTH
                        }
                        NavGraph(navController = navController, startDestination = startDestination)
                    }
                }
            }
        }
    }
}
