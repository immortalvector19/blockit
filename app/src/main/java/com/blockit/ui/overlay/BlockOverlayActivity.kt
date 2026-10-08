package com.blockit.ui.overlay

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import com.blockit.core.BlockItApplication
import com.blockit.domain.model.SessionStrictness
import com.blockit.ui.theme.BlockItTheme

class BlockOverlayActivity : ComponentActivity() {

    private var currentTarget: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = BlockItApplication.from(this)

        val target = intent.getStringExtra(EXTRA_TARGET) ?: "Blocked App"
        currentTarget = target
        isShowing = true
        currentBlockedTarget = target
        val profile = intent.getStringExtra(EXTRA_PROFILE) ?: "Active Profile"
        val reason = intent.getStringExtra(EXTRA_REASON) ?: "Focus mode active"
        val strictnessStr = intent.getStringExtra(EXTRA_STRICTNESS) ?: SessionStrictness.NORMAL.name
        val strictness = try {
            SessionStrictness.valueOf(strictnessStr)
        } catch (_: Exception) {
            SessionStrictness.NORMAL
        }
        val canRequestPause = intent.getBooleanExtra(EXTRA_CAN_PAUSE, true)
        val remainingPauses = intent.getIntExtra(EXTRA_REMAINING_PAUSES, 3)
        val maxPauses = intent.getIntExtra(EXTRA_MAX_PAUSES, 3)

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
                    strictness = strictness,
                    canRequestPause = canRequestPause,
                    remainingPauses = remainingPauses,
                    maxPauses = maxPauses,
                    onHomeClicked = {
                        kickToHome()
                    },
                    onGrantPause = { minutes ->
                        val durationMs = minutes * 60 * 1000L
                        container.intentPassManager.grantPass(target, minutes)

                        val appLabel = try {
                            val appInfo = packageManager.getApplicationInfo(target, 0)
                            packageManager.getApplicationLabel(appInfo).toString()
                        } catch (_: Exception) {
                            target
                        }

                        container.overlayController.showPauseCountdownPill(
                            appName = appLabel,
                            expiresAtMs = System.currentTimeMillis() + durationMs,
                            onEndPauseEarly = {
                                container.intentPassManager.revokePass(target)
                                container.overlayController.hidePauseCountdownPill()
                                kickToHome()
                            }
                        )

                        val launchIntent = packageManager.getLaunchIntentForPackage(target)
                        launchIntent?.let {
                            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                            startActivity(it)
                        }
                        finish()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isShowing = false
        if (currentBlockedTarget == currentTarget) {
            currentBlockedTarget = null
        }
    }

    private fun kickToHome() {
        isShowing = false
        currentBlockedTarget = null
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    companion object {
        @Volatile
        var isShowing: Boolean = false
            private set

        @Volatile
        var currentBlockedTarget: String? = null
            private set

        private const val EXTRA_TARGET = "extra_target"
        private const val EXTRA_PROFILE = "extra_profile"
        private const val EXTRA_REASON = "extra_reason"
        private const val EXTRA_STRICTNESS = "extra_strictness"
        private const val EXTRA_CAN_PAUSE = "extra_can_pause"
        private const val EXTRA_REMAINING_PAUSES = "extra_remaining_pauses"
        private const val EXTRA_MAX_PAUSES = "extra_max_pauses"

        fun start(
            context: Context,
            target: String,
            profile: String,
            reason: String,
            strictness: SessionStrictness = SessionStrictness.NORMAL,
            canRequestPause: Boolean = true,
            remainingPauses: Int = 3,
            maxPauses: Int = 3
        ) {
            // Guard: If already showing the block screen for this target, avoid redundant re-launches
            if (isShowing && currentBlockedTarget == target) {
                return
            }

            val intent = Intent(context, BlockOverlayActivity::class.java).apply {
                putExtra(EXTRA_TARGET, target)
                putExtra(EXTRA_PROFILE, profile)
                putExtra(EXTRA_REASON, reason)
                putExtra(EXTRA_STRICTNESS, strictness.name)
                putExtra(EXTRA_CAN_PAUSE, canRequestPause)
                putExtra(EXTRA_REMAINING_PAUSES, remainingPauses)
                putExtra(EXTRA_MAX_PAUSES, maxPauses)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
            }
            context.startActivity(intent)
        }
    }
}
