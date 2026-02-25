package io.lifephysics.architect2.ui.composables

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * An animated overlay that displays coins gained or lost.
 *
 * On a critical hit, the amount is larger and rendered in bright orange-gold
 * instead of standard gold. No label text is shown — size and color are the signal.
 *
 * @param amount The coin amount. Positive for gain, negative for loss.
 * @param isCritical True if the coin gain was a critical hit.
 * @param onDismiss Called when the animation completes.
 * @param modifier Modifier for positioning the pop-up within its parent.
 */
@Composable
fun CoinPopup(
    amount: Int,
    isCritical: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val yOffset = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        yOffset.animateTo(
            targetValue = -150f,
            animationSpec = tween(durationMillis = 1800)
        )
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 500, delayMillis = 1300)
        )
        onDismiss()
    }

    val text = if (amount >= 0) "+${amount}" else "${amount}"

    val color = when {
        isCritical -> Color(0xFFFF8F00) // Deep amber — visually distinct from standard gold
        amount >= 0 -> Color(0xFFFFD700) // Standard gold
        else -> Color(0xFFF44336)        // Red for loss
    }

    val iconSize = if (isCritical) 36.dp else 28.dp
    val fontSize = if (isCritical) 30.sp else 24.sp

    Row(
        modifier = modifier
            .offset(y = yOffset.value.dp)
            .alpha(alpha.value)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.MonetizationOn,
            contentDescription = "Coins",
            tint = color,
            modifier = Modifier.size(iconSize)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.headlineSmall
        )
    }
}
