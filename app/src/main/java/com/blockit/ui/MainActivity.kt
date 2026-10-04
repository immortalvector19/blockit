package com.blockit.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.blockit.core.BlockItApplication
import com.blockit.ui.navigation.BlockItNavHost
import com.blockit.ui.theme.BlockItTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = BlockItApplication.from(this)

        setContent {
            BlockItTheme {
                val navController = rememberNavController()
                BlockItNavHost(
                    navController = navController,
                    container = container
                )
            }
        }
    }
}
