package com.blockit.ui.overlay

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import com.blockit.ui.theme.BlockItTheme

class BlockOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val target = intent.getStringExtra(EXTRA_TARGET) ?: "Blocked App"
        val profile = intent.getStringExtra(EXTRA_PROFILE) ?: "Active Profile"
        val reason = intent.getStringExtra(EXTRA_REASON) ?: "Focus mode active"

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                kickToHome()
            }
        })

        setContent {
            BlockItTheme(darkTheme = true) {
                BlockScreenContent(
                    targetIdentifier = target,
                    profileName = profile,
                    reason = reason,
                    onHomeClicked = {
                        kickToHome()
                    }
                )
            }
        }
    }

    private fun kickToHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    companion object {
        private const val EXTRA_TARGET = "extra_target"
        private const val EXTRA_PROFILE = "extra_profile"
        private const val EXTRA_REASON = "extra_reason"

        fun start(context: Context, target: String, profile: String, reason: String) {
            val intent = Intent(context, BlockOverlayActivity::class.java).apply {
                putExtra(EXTRA_TARGET, target)
                putExtra(EXTRA_PROFILE, profile)
                putExtra(EXTRA_REASON, reason)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
            }
            context.startActivity(intent)
        }
    }
}
