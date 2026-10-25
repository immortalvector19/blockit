package com.blockit.ui.overlay

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.blockit.core.BlockItApplication
import com.blockit.domain.model.SessionStrictness
import com.blockit.ui.theme.BlockItTheme
import kotlinx.coroutines.launch

class BlockOverlayActivity : ComponentActivity() {

    private var currentTarget: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                kickToHome()
            }
        })
        handleOverlayIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent?.let { handleOverlayIntent(it) }
    }

    override fun onStart() {
        super.onStart()
        isShowing = true
        currentBlockedTarget = currentTarget
    }

    override fun onResume() {
        super.onResume()
        isShowing = true
        currentBlockedTarget = currentTarget
    }

    override fun onStop() {
        super.onStop()
        isShowing = false
        if (currentBlockedTarget == currentTarget) {
            currentBlockedTarget = null
        }
        if (!isChangingConfigurations) {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isShowing = false
        if (currentBlockedTarget == currentTarget) {
            currentBlockedTarget = null
        }
    }

    private fun handleOverlayIntent(intent: Intent) {
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
        val sessionEndMs = intent.getLongExtra(EXTRA_SESSION_END_MS, 0L)

        setContent {
            BlockItTheme(darkTheme = true) {
                val snapshot by container.ruleEvaluator.snapshotFlow.collectAsState()
                val activeSession by androidx.compose.runtime.remember { container.focusSessionManager.observeActiveSession() }.collectAsState(
                    initial = container.focusSessionManager.getCachedActiveSession()
                )
                val effectiveSessionEndMs = activeSession?.endTimestamp
                    ?: snapshot.activeSessionEndTimestampMs
                    ?: snapshot.activeScheduleEndTimestampMs
                    ?: sessionEndMs

                val isBreakActive by container.intentPassManager.isBreakActiveFlow.collectAsState()
                val breakExpiresAtMs by container.intentPassManager.breakExpiresAtMsFlow.collectAsState()
                val todayGoals by container.focusGoalRepository.observeTodayGoals().collectAsState(initial = emptyList())

                val profileEntity by androidx.compose.runtime.produceState<com.blockit.data.local.db.entity.ProfileEntity?>(
                    initialValue = null,
                    key1 = profile
                ) {
                    value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        container.database.profileDao().getProfileByName(profile)
                    }
                }

                val bgType = try {
                    com.blockit.domain.model.OverlayBackgroundType.valueOf(
                        profileEntity?.overlayBackgroundType ?: "DEFAULT"
                    )
                } catch (_: Exception) {
                    com.blockit.domain.model.OverlayBackgroundType.DEFAULT
                }
                val imageUris = profileEntity?.overlayImageUris?.split("|")?.filter { it.isNotBlank() } ?: emptyList()
                val customMantra = profileEntity?.overlayCustomText

                BlockScreenContent(
                    targetIdentifier = target,
                    profileName = profile,
                    reason = reason,
                    strictness = strictness,
                    canRequestPause = canRequestPause,
                    remainingPauses = remainingPauses,
                    maxPauses = maxPauses,
                    sessionEndMs = effectiveSessionEndMs,
                    scheduleEndMs = snapshot.activeScheduleEndTimestampMs,
                    isBreakActive = isBreakActive,
                    breakExpiresAtMs = breakExpiresAtMs,
                    overlayBackgroundType = bgType,
                    overlayImageUris = imageUris,
                    overlayCustomText = customMantra,
                    goals = todayGoals,
                    onToggleGoal = { goalId, isCompleted ->
                        container.applicationScope.launch {
                            container.focusGoalRepository.toggleGoalCompletion(goalId, isCompleted)
                        }
                    },
                    onAddGoal = { title ->
                        container.applicationScope.launch {
                            container.focusGoalRepository.addGoal(
                                title = title,
                                targetDate = com.blockit.data.repository.FocusGoalRepository.getTodayIsoDate(),
                                sessionId = activeSession?.id
                            )
                        }
                    },
                    onHomeClicked = {
                        kickToHome()
                    },
                    onGrantPause = { minutes ->
                        val durationMs = minutes * 60 * 1000L
                        container.intentPassManager.grantPass(target, minutes)
                        container.intentPassManager.grantBreak(minutes)

                        val appLabel = try {
                            val appInfo = packageManager.getApplicationInfo(target, 0)
                            packageManager.getApplicationLabel(appInfo).toString()
                        } catch (_: Exception) {
                            target
                        }

                        if (container.overlayController.canDrawOverlays()) {
                            container.overlayController.showPauseCountdownPill(
                                appName = appLabel,
                                expiresAtMs = System.currentTimeMillis() + durationMs,
                                onEndPauseEarly = {
                                    container.intentPassManager.revokePass(target)
                                    container.intentPassManager.endBreak()
                                    container.overlayController.hidePauseCountdownPill()
                                    kickToHome()
                                }
                            )
                        }

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
        private const val EXTRA_SESSION_END_MS = "extra_session_end_ms"

        fun start(
            context: Context,
            target: String,
            profile: String,
            reason: String,
            strictness: SessionStrictness = SessionStrictness.NORMAL,
            canRequestPause: Boolean = true,
            remainingPauses: Int = 3,
            maxPauses: Int = 3,
            sessionEndMs: Long = 0L
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
                putExtra(EXTRA_SESSION_END_MS, sessionEndMs)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
            }
            context.startActivity(intent)
        }
    }
}
