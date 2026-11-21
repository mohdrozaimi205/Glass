package com.mohdrozaimi.glass

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import com.mohdrozaimi.glass.ui.GlassRoot
import com.mohdrozaimi.glass.ui.HomeViewModel
import com.mohdrozaimi.glass.ui.WidgetController

class MainActivity : ComponentActivity() {
    private val vm: HomeViewModel by viewModels()
    private lateinit var widgets: WidgetController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        widgets = WidgetController(this)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                GlassRoot(vm, widgets)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        try { widgets.host.startListening() } catch (e: Exception) { }
    }

    override fun onStop() {
        super.onStop()
        try { widgets.host.stopListening() } catch (e: Exception) { }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshAll()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        vm.onHomePressed()
    }

    @Deprecated("Diperlukan untuk kebenaran & konfigurasi widget")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        widgets.onResult(requestCode, resultCode == RESULT_OK)
    }
}
