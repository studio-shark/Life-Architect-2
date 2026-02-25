package io.lifephysics.architect2.domain

/**
 * Represents a single purchasable avatar in the shop.
 *
 * @param id Unique identifier, 1–50. Also used to look up the drawable resource.
 * @param name The display name shown in the shop grid.
 * @param price The coin cost to purchase this avatar.
 * @param drawableRes The R.drawable resource ID for the avatar image.
 */
data class Avatar(
    val id: Int,
    val name: String,
    val price: Int,
    val drawableRes: Int
)
