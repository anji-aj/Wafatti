package com.aj.wafatti

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.aj.wafatti.core.navigation.WafattiNavHost
import com.aj.wafatti.data.repository.PresenceManager
import com.aj.wafatti.ui.theme.WafattiTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var presenceManager: PresenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenceManager.initialize()
        enableEdgeToEdge()
        setContent {
            WafattiTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    WafattiNavHost(navController = navController)
                }
            }
        }
    }
}
