package com.autoslash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.autoslash.session.SessionManager
import com.autoslash.ui.MainScreen
import com.autoslash.ui.MainViewModel
import com.autoslash.ui.theme.AutoSlashTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AutoSlashTheme {
                MainScreen(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshAccessibility()
    }

    override fun onDestroy() {
        // The user closed the app (back / finish): never leave automation running behind it.
        if (isFinishing) SessionManager.stop()
        super.onDestroy()
    }
}
