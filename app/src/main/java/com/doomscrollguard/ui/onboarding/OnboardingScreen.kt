package com.doomscrollguard.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.doomscrollguard.data.Settings
import com.doomscrollguard.data.SettingsRepository
import com.doomscrollguard.service.ServiceStatus
import com.doomscrollguard.ui.home.AppsSection
import com.doomscrollguard.ui.home.BatteryHelpCard
import com.doomscrollguard.ui.home.GoalSection
import com.doomscrollguard.ui.home.RestrictedSettingsHelp
import com.doomscrollguard.ui.home.StatusCard
import com.doomscrollguard.ui.home.openAccessibilitySettings
import com.doomscrollguard.ui.theme.Leaf
import kotlinx.coroutines.launch

private enum class Step { WELCOME, HOW, PRIVACY, APPS, ENABLE, FINISH }

@Composable
fun OnboardingScreen(settings: Settings, repo: SettingsRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    val step = Step.entries[stepIndex]

    var serviceEnabled by remember { mutableStateOf(ServiceStatus.isEnabled(context)) }
    LifecycleResumeEffect(Unit) {
        serviceEnabled = ServiceStatus.isEnabled(context)
        onPauseOrDispose { }
    }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    fun next() {
        if (stepIndex < Step.entries.lastIndex) stepIndex++
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
    ) {
        ProgressDots(current = stepIndex, total = Step.entries.size)
        Spacer(Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (step) {
                Step.WELCOME -> {
                    BigEmoji("🌱")
                    Title("Take your time back")
                    Body(
                        "Endless feeds are designed to keep you scrolling. Minutes turn into hours " +
                            "without you noticing.\n\nDoomScroll Guard notices when you've been scrolling " +
                            "too long and gently helps you stop.",
                    )
                }
                Step.HOW -> {
                    Title("How it works")
                    Level("⏳", "1. A gentle nudge", "After 10 minutes of scrolling, a small card reminds you how long it's been.")
                    Level("🫁", "2. A pause", "Still going 5 minutes later? A full-screen pause asks you to take a breath.")
                    Level("🧭", "3. Your intention", "Keep going and you'll need to write down what you're looking for.")
                    Body("You're always in control. You can keep scrolling, it just takes a moment of thought.")
                }
                Step.PRIVACY -> {
                    BigEmoji("🔒")
                    Title("Your privacy")
                    Body(
                        "DoomScroll Guard uses Android's Accessibility service to:\n\n" +
                            "• See which app is open\n" +
                            "• Notice when you scroll in the apps you choose\n" +
                            "• Show reminders on top of those apps\n\n" +
                            "It does NOT read text, messages, or anything on your screen. " +
                            "It has no internet access. Everything stays on your phone.",
                    )
                }
                Step.APPS -> {
                    Title("Which apps pull you in?")
                    Body("Pick the apps where you lose track of time.")
                    AppsSection(settings.monitoredPackages) { pkgs, on ->
                        scope.launch { pkgs.forEach { repo.setMonitored(it, on) } }
                    }
                }
                Step.ENABLE -> {
                    Title("Turn on protection")
                    Body("In the next screen, find DoomScroll Guard and switch it on, then come back here.")
                    StatusCard(enabled = serviceEnabled, onEnable = { openAccessibilitySettings(context) })
                    if (!serviceEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        RestrictedSettingsHelp()
                    }
                    if (serviceEnabled && ServiceStatus.needsBatteryHelp(context)) {
                        BatteryHelpCard()
                    }
                }
                Step.FINISH -> {
                    BigEmoji("🎯")
                    Title("Set your goal")
                    GoalSection(settings.dailyGoalMinutes) { scope.launch { repo.setDailyGoal(it) } }
                    Body("Each evening at 9 PM you'll get a short recap of your day. You can turn it off anytime.")
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (stepIndex > 0) {
                TextButton(onClick = { stepIndex-- }) { Text("Back") }
            }
            Spacer(Modifier.weight(1f))
            when (step) {
                Step.PRIVACY -> Button(
                    onClick = {
                        scope.launch { repo.acceptDisclosure() }
                        next()
                    },
                ) { Text("I agree") }
                Step.APPS ->
                    if (settings.monitoredPackages.isNotEmpty()) {
                        Button(onClick = ::next) { Text("Next") }
                    } else {
                        TextButton(onClick = ::next) { Text("Skip for now") }
                    }
                Step.ENABLE ->
                    if (serviceEnabled) {
                        Button(onClick = ::next) { Text("Next") }
                    } else {
                        TextButton(onClick = ::next) { Text("Skip for now") }
                    }
                Step.FINISH -> Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        scope.launch { repo.completeOnboarding() }
                    },
                ) { Text("Start") }
                else -> Button(onClick = ::next) { Text("Next") }
            }
        }
    }
}

@Composable
private fun ProgressDots(current: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            Box(
                Modifier
                    .size(if (i == current) 10.dp else 8.dp)
                    .background(
                        if (i <= current) Leaf else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun BigEmoji(emoji: String) {
    Text(emoji, fontSize = 56.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
}

@Composable
private fun Title(text: String) {
    Text(text, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun Body(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Level(emoji: String, title: String, body: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(emoji, fontSize = 28.sp)
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
