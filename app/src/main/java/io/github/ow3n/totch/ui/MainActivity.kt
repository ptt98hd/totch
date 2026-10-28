package io.github.ow3n.totch.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.ow3n.totch.TotchApp
import io.github.ow3n.totch.ui.navigation.TotchNavHost
import io.github.ow3n.totch.ui.theme.TotchTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as TotchApp).container
        setContent {
            TotchTheme {
                TotchNavHost(container)
            }
        }
    }
}
