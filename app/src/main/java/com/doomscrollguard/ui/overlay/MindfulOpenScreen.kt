package com.doomscrollguard.ui.overlay

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doomscrollguard.ui.theme.CalmDeep
import com.doomscrollguard.ui.theme.Leaf

enum class MindfulChoice {
    /** Came for something specific: normal thresholds. */
    PURPOSE,
    /** Admitted to just browsing: first reminder comes sooner. */
    BROWSING,
    /** Changed their mind: go home. */
    NEVER_MIND,
}

/** Shown when opening a watched app fresh: a 2-second moment of intention. */
@Composable
fun MindfulOpenScreen(appLabel: String, onChoice: (MindfulChoice) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CalmDeep, RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("👋", fontSize = 36.sp)
            Text(
                "What brings you to $appLabel?",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = { onChoice(MindfulChoice.PURPOSE) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Leaf),
            ) { Text("Something specific", color = Color.White) }
            OutlinedButton(
                onClick = { onChoice(MindfulChoice.BROWSING) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Just browsing", color = Color.White) }
            TextButton(onClick = { onChoice(MindfulChoice.NEVER_MIND) }) {
                Text("Never mind, close it", color = Color.White.copy(alpha = 0.7f))
            }
            Text(
                "\"Just browsing\" means we'll check in sooner.",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
