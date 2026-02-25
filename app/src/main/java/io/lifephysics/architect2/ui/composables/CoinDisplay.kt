package io.lifephysics.architect2.ui.composables

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A reusable composable that renders a gold coin icon followed by a numeric value.
 *
 * Use this everywhere coins are displayed — the User profile stats, the Shop grid cells,
 * purchase buttons, and any future inventory screens. This ensures a single source of
 * truth for the coin visual identity across the entire app.
 *
 * @param amount The coin amount to display.
 * @param modifier Modifier applied to the outer Row.
 * @param textStyle The text style to use. Defaults to the current local text style.
 * @param iconSize The size of the coin icon. Defaults to 16.dp to match body text.
 */
@Composable
fun CoinDisplay(
    amount: Int,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LocalTextStyle.current,
    iconSize: Dp = 16.dp
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.MonetizationOn,
            contentDescription = "Coins",
            tint = Color(0xFFFFD700), // Gold — consistent with the CoinPopup
            modifier = Modifier.size(iconSize)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = amount.toString(),
            style = textStyle,
            fontWeight = FontWeight.SemiBold
        )
    }
}
