package io.lifephysics.architect2.ui.composables

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * An animated overlay composable that displays XP gained or lost.
 *
 * The text floats upward and fades out over ~1.5 seconds.
 * A critical hit displays a larger, yellow message.
 * An XP loss displays a red message with a negative value.
 *
 * @param amount The XP amount. Positive for gain, negative for loss.
 * @param isCritical True if the gain was a critical hit.
 * @param onDismiss Called when the animation completes.
 * @param modifier Modifier for positioning the pop-up within its parent.
 */
@Composable
fun XpPopup(
    amount: Int,
    isCritical: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val yOffset = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // Float upward while fading out
        yOffset.animateTo(
            targetValue = -120f,
            animationSpec = tween(durationMillis = 1500)
        )
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 400)
        )
        onDismiss()
    }

    val text = when {
        isCritical -> "CRITICAL HIT!\n+${amount} XP"
        amount >= 0 -> "+${amount} XP"
        else -> "${amount} XP"
    }

    val color = when {
        isCritical -> Color(0xFFFFD700) // Gold
        amount >= 0 -> Color(0xFF4CAF50) // Green
        else -> Color(0xFFF44336)        // Red
    }

    val fontSize = if (isCritical) 26.sp else 22.sp

    Box(
        modifier = modifier
            .offset(y = yOffset.value.dp)
            .alpha(alpha.value)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall
        )
    }
}
