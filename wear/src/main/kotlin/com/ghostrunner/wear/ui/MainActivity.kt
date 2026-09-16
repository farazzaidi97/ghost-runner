package com.ghostrunner.wear.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ghostrunner.wear.GhostRunnerWearApplication
import com.ghostrunner.wear.ui.navigation.GhostRunnerWearNavGraph
import com.ghostrunner.wear.ui.theme.GhostRunnerWearTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as GhostRunnerWearApplication).container

        setContent {
            GhostRunnerWearTheme {
                GhostRunnerWearNavGraph(container = container)
            }
        }
    }
}
