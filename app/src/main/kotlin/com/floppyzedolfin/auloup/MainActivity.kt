package com.floppyzedolfin.auloup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.floppyzedolfin.auloup.data.PrefixRepository
import com.floppyzedolfin.auloup.data.ThemeMode
import com.floppyzedolfin.auloup.service.Notifications
import com.floppyzedolfin.auloup.telephony.PhoneFormat
import com.floppyzedolfin.auloup.ui.AuLoupScreen
import com.floppyzedolfin.auloup.ui.IrisBackdrop

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Notifications.ensureChannel(this)
        PhoneFormat.init(this)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val repository = remember { PrefixRepository(context.applicationContext) }
            val themeMode by repository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val dark = ThemeMode.isDark(themeMode, isSystemInDarkTheme())
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                IrisBackdrop(dark = dark) {
                    AuLoupScreen(repository)
                }
            }
        }
    }
}
