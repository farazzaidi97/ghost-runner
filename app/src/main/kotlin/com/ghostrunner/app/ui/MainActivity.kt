package com.ghostrunner.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.ghostrunner.app.GhostRunnerApplication
import com.ghostrunner.app.ui.navigation.GhostRunnerNavGraph
import com.ghostrunner.app.ui.theme.GhostRunnerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as GhostRunnerApplication).container

        setContent {
            GhostRunnerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GhostRunnerNavGraph(container = container)
                }
            }
        }
    }
}
