package com.doomscrollguard.ui.overlay

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doomscrollguard.ui.theme.Calm
import com.doomscrollguard.ui.theme.CalmDeep
import com.doomscrollguard.ui.theme.Leaf
import kotlinx.coroutines.delay

private const val BREATH_PHASE_MS = 4_000L

/** Everything an intervention screen needs to display, already formatted. */
data class InterventionCopy(
    val appLabel: String,
    val duration: String,
    val gestures: Int,
    val distance: String,
    /** Total feed time today across all apps, if known. */
    val todayTotal: String? = null,
)

/** Level 1: a gentle card at the top of the screen. The app stays usable underneath. */
@Composable
fun NudgeCard(copy: InterventionCopy, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(12.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CalmDeep),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("⏳", fontSize = 28.sp)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    "${copy.duration} on ${copy.appLabel}",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
                Text(
                    "${copy.gestures} swipes · ${copy.distance} of feed. Still enjoying this, or just scrolling?",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                )
            }
            TextButton(onClick = onDismiss) {
                Text("OK", color = Calm)
            }
        }
    }
}

/**
 * Level 2: a full-screen pause with a breathing animation. "Continue" is locked
 * for [lockSeconds] so the user has to actually stop for a moment.
 */
@Composable
fun PauseScreen(
    copy: InterventionCopy,
    lockSeconds: Int = 10,
    onLeave: () -> Unit,
    onContinue: () -> Unit,
) {
    var remaining by remember { mutableIntStateOf(lockSeconds) }
    LaunchedEffect(Unit) {
        while (remaining > 0) {
            delay(1_000)
            remaining--
        }
    }

    val breath = rememberInfiniteTransition(label = "breath")
    val scale by breath.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = BREATH_PHASE_MS.toInt(), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathScale",
    )
    // Flips in step with the 4s tween above: grow = breathe in, shrink = breathe out.
    var inhaling by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(BREATH_PHASE_MS)
            inhaling = !inhaling
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CalmDeep.copy(alpha = 0.97f))
            // Swallow touches so the feed underneath can't be scrolled.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .systemBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "Take a breath.",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(32.dp))

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .scale(scale)
                        .background(Calm.copy(alpha = 0.25f), CircleShape),
                )
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(scale)
                        .background(Calm.copy(alpha = 0.5f), CircleShape),
                )
                Text(
                    if (inhaling) "in" else "out",
                    color = Color.White,
                    fontSize = 18.sp,
                )
            }

            Spacer(Modifier.height(32.dp))
            Text(
                "${copy.duration} on ${copy.appLabel}",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "${copy.gestures} swipes · ${copy.distance} of feed",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 15.sp,
            )
            copy.todayTotal?.let {
                Text("Today so far: $it", color = Color.White.copy(alpha = 0.7f), fontSize = 15.sp)
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Is this how you want to spend your time right now?",
                color = Color.White,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(40.dp))

            Button(
                onClick = onLeave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Leaf),
            ) {
                Text("Leave ${copy.appLabel}", fontSize = 17.sp, color = Color.White)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onContinue, enabled = remaining == 0) {
                Text(
                    if (remaining > 0) "Continue scrolling ($remaining)" else "Continue scrolling",
                    color = if (remaining > 0) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.8f),
                )
            }
        }
    }
}
