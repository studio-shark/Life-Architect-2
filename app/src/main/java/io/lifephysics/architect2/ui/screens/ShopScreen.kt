package io.lifephysics.architect2.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.lifephysics.architect2.domain.AvatarCatalog
import io.lifephysics.architect2.ui.composables.AvatarGridItem
import io.lifephysics.architect2.ui.composables.CoinDisplay
import io.lifephysics.architect2.ui.viewmodel.MainUiState
import io.lifephysics.architect2.ui.viewmodel.MainViewModel

/**
 * The Avatar Shop screen.
 *
 * Displays all avatars from [AvatarCatalog] in a 3-column grid.
 * Each cell reflects the avatar's current state: locked, owned, or equipped.
 *
 * The screen is stateless — all state is read from [uiState] and all actions
 * are delegated to [viewModel]. This makes it trivial to scale to thousands
 * of avatars by simply expanding [AvatarCatalog.all].
 */
@Composable
fun ShopScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val userCoins = uiState.user?.coins ?: 0

    Column(modifier = modifier.fillMaxSize()) {

        // --- Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Avatar Shop",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            CoinDisplay(
                amount = userCoins,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground
                ),
                iconSize = 18.dp
            )
        }

        HorizontalDivider()

        // --- Avatar Grid ---
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = AvatarCatalog.all,
                key = { avatar -> avatar.id }
            ) { avatar ->
                val isOwned    = avatar.id in uiState.ownedAvatarIds
                val isEquipped = avatar.id == uiState.equippedAvatarId
                val canAfford  = userCoins >= avatar.price

                AvatarGridItem(
                    avatar     = avatar,
                    isOwned    = isOwned,
                    isEquipped = isEquipped,
                    canAfford  = canAfford,
                    onPurchase = { viewModel.onPurchaseAvatar(avatar) },
                    onEquip    = { viewModel.onEquipAvatar(avatar) }
                )
            }
        }
    }
}
