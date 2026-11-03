package com.doomscrollguard.ui.overlay

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doomscrollguard.ui.theme.CalmDeep
import com.doomscrollguard.ui.theme.Leaf
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Shown instead of the feed when a watched app is reopened during a break.
 * [endsAtElapsed] is a SystemClock.elapsedRealtime() value. [skipAfterSeconds]
 * null means the break can't be skipped (Strict).
 */
@Composable
fun BreakScreen(
    appLabel: String,
    endsAtElapsed: Long,
    skipAfterSeconds: Int?,
    onGoHome: () -> Unit,
    onOpenAnyway: () -> Unit,
    onFinished: () -> Unit,
) {
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val shownAt = remember { now }
    LaunchedEffect(Unit) {
        while (now < endsAtElapsed) {
            delay(250)
            now = SystemClock.elapsedRealtime()
        }
        onFinished()
    }
    val remainingSec = ((endsAtElapsed - now).coerceAtLeast(0) + 999) / 1000
    val skipIn = skipAfterSeconds?.let { it - ((now - shownAt) / 1000).toInt() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CalmDeep)
            // Swallow touches so the app underneath can't be used.
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
            Text("🌿", fontSize = 56.sp)
            Spacer(Modifier.height(16.dp))
            Text("Break time", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "You chose to step away. $appLabel will be back in",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                String.format(Locale.US, "%d:%02d", remainingSec / 60, remainingSec % 60),
                color = Leaf,
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Stretch, drink some water, look out a window. The feed will still be there.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(40.dp))

            Button(
                onClick = onGoHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Leaf),
            ) {
                Text("OK, go home", fontSize = 17.sp, color = Color.White)
            }
            Spacer(Modifier.height(8.dp))
            when {
                skipIn == null -> Text(
                    "Strict mode: breaks can't be skipped.",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 13.sp,
                )
                skipIn > 0 -> TextButton(onClick = {}, enabled = false) {
                    Text("Open anyway ($skipIn)", color = Color.White.copy(alpha = 0.35f))
                }
                else -> TextButton(onClick = onOpenAnyway) {
                    Text("Open anyway", color = Color.White.copy(alpha = 0.7f))
                }
            }
        }
    }
}
