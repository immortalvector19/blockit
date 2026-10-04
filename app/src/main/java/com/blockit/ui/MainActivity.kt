package com.blockit.ui

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.blockit.core.BlockItApplication
import com.blockit.ui.navigation.BlockItNavHost
import com.blockit.ui.theme.BlockItTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.setBackgroundColor(Color.parseColor("#0B1320"))
        val container = BlockItApplication.from(this)

        setContent {
            BlockItTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    BlockItNavHost(
                        navController = navController,
                        container = container
                    )
                }
            }
        }
    }
}
