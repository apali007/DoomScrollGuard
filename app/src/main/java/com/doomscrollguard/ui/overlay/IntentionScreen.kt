package com.doomscrollguard.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doomscrollguard.ui.theme.Calm
import com.doomscrollguard.ui.theme.CalmDeep
import com.doomscrollguard.ui.theme.Leaf
import kotlinx.coroutines.delay

/** Minimum words the user must write before "Continue" unlocks. */
const val INTENTION_MIN_WORDS = 3

fun wordCount(text: String): Int = text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }

/**
 * Level 3: to keep scrolling, the user has to put into words why. Typing a
 * reason breaks autopilot far better than tapping a button.
 */
@Composable
fun IntentionScreen(
    copy: InterventionCopy,
    lockSeconds: Int = 15,
    onLeave: () -> Unit,
    onContinue: (reason: String) -> Unit,
) {
    var remaining by remember { mutableIntStateOf(lockSeconds) }
    var reason by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (remaining > 0) {
            delay(1_000)
            remaining--
        }
    }
    val words = wordCount(reason)
    val canContinue = remaining == 0 && words >= INTENTION_MIN_WORDS

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CalmDeep)
            .systemBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("🧭", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "${copy.duration} on ${copy.appLabel}",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                listOfNotNull(
                    "${copy.gestures} swipes · ${copy.distance}",
                    copy.todayTotal?.let { "today: $it" },
                ).joinToString(" · "),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            Text(
                "What are you looking for right now?",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                "Write it down to keep going. If you can't name it, maybe it's time to stop.",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it.take(200) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. replying to my friend's message") },
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Calm,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    cursorColor = Calm,
                    focusedPlaceholderColor = Color.White.copy(alpha = 0.35f),
                    unfocusedPlaceholderColor = Color.White.copy(alpha = 0.35f),
                ),
            )
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onLeave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Leaf),
            ) {
                Text("I'm done, leave ${copy.appLabel}", fontSize = 17.sp, color = Color.White)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { onContinue(reason.trim()) }, enabled = canContinue) {
                val label = when {
                    remaining > 0 -> "Continue ($remaining)"
                    words < INTENTION_MIN_WORDS -> "Continue (write $INTENTION_MIN_WORDS+ words)"
                    else -> "Continue scrolling"
                }
                Text(
                    label,
                    color = if (canContinue) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.35f),
                )
            }
        }
    }
}
