package com.floppyzedolfin.auloup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.floppyzedolfin.auloup.R
import com.floppyzedolfin.auloup.data.PrefixRepository
import com.floppyzedolfin.auloup.data.ThemeMode
import com.floppyzedolfin.auloup.service.Notifications
import com.floppyzedolfin.auloup.telephony.PhoneFormat
import com.floppyzedolfin.auloup.ui.AuLoupScreen

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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    // Iris mesh as a faint full-page backdrop behind every screen
                    // (each screen's Scaffold is transparent). Theme-aware artwork.
                    Image(
                        painter = painterResource(if (dark) R.drawable.iris_dark else R.drawable.iris_light),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alpha = 0.20f,
                        modifier = Modifier.fillMaxSize(),
                    )
                    AuLoupScreen(repository)
                }
            }
        }
    }
}
