package com.hellacious.spirecart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.hellacious.spirecart.ui.navigation.SpireCartNavGraph
import com.hellacious.spirecart.ui.theme.SpireCartTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as SpireCartApplication).container

        setContent {
            SpireCartTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SpireCartNavGraph(appContainer = appContainer)
                }
            }
        }
    }
}