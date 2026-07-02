package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle.Companion.Italic
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.jeffreyalanwang.dutchrailwaysandroidclient.R
import com.jeffreyalanwang.dutchrailwaysandroidclient.TrainAmenity
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.AppIcons
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.Gold
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.shift

@Preview(widthDp = 300, heightDp = 500)
@Composable
private fun AmenityBadgePreview() {
    var amenities by remember { mutableStateOf(TrainAmenity.entries.toSet()) }
    var isExpanded by remember { mutableStateOf(true) }
    Box(Modifier.size(300.dp, 200.dp)) {
        Box(
            Modifier
                .fillMaxSize()
                .clickable { isExpanded = !isExpanded },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
            ) {
                Icon(
                    painterResource(R.drawable.ic_dr_trainservice),
                    "Train icon",
                    Modifier
                        .size(72.dp + 20.dp)
                )

                // Test that we are not clipped by nested layout
                EditAmenityBadgeSet(
                    amenities,
                    onModify = { amenities = it },
                    isExpanded = isExpanded,
                    onSetExpanded = { isExpanded = it },
                    containerModifier = Modifier
                        .shift(-25.dp, -7.5.dp),
                    windowInsets = WindowInsets(top = 20.dp, right = 20.dp),
                )

                // Test that we display expanded badges on top
                Icon(
                    painterResource(R.drawable.ic_draghandle_vertical),
                    "Test content",
                    Modifier
                        .shift(-100.dp, 0.dp)
                        .size(72.dp + 20.dp),
                    tint = Color.Blue,
                )
            }
        }
    }
}

/**
 * An editable set of amenity badges that can expand to show labels and allow adding or removing amenities.
 *
 * @param amenities The set of currently selected amenities.
 * @param onModify Callback invoked when the set of amenities is modified (added or removed).
 * @param isExpanded Whether the badge set is currently in its expanded state.
 * @param onSetExpanded Callback to toggle the expansion state.
 * @param windowInsets The insets used for positioning the expanded popup.
 * @param contentModifier Modifier applied to the inner layout.
 * @param containerModifier Modifier applied to the outer placeholder container.
 * @param collapsedBadgeSize The size of the badges when collapsed.
 * @param expandedBadgeSize The size of the badges when expanded.
 * @param color The primary color for the badge icons and borders.
 * @param bgColor The background color of the badges.
 */
@SuppressLint("ModifierParameter")
@Composable
fun EditAmenityBadgeSet(
    amenities: Set<TrainAmenity>,
    onModify: ((Set<TrainAmenity>) -> Unit),
    isExpanded: Boolean,
    onSetExpanded: (Boolean) -> Unit,
    windowInsets: WindowInsets,
    contentModifier: Modifier = Modifier,
    containerModifier: Modifier = Modifier,
    collapsedBadgeSize: Dp = 15.dp,
    expandedBadgeSize: Dp = 30.dp,
    color: Color = LocalContentColor.current,
) = AmenityBadgeSetBase(
    amenities,
    isExpanded,
    onSetExpanded,
    windowInsets = windowInsets,
    contentModifier = contentModifier,
    containerModifier = containerModifier,
    onModify = onModify,
    collapsedBadgeSize = collapsedBadgeSize,
    expandedBadgeSize = expandedBadgeSize,
    color,
)

/**
 * A read-only set of amenity badges that can expand to show labels.
 *
 * @param amenities The set of amenities to display.
 * @param isExpanded Whether the badge set is currently in its expanded state.
 * @param onSetExpanded Callback to toggle the expansion state.
 * @param windowInsets The insets used for positioning the expanded popup.
 * @param contentModifier Modifier applied to the inner layout.
 * @param containerModifier Modifier applied to the outer placeholder container.
 * @param collapsedBadgeSize The size of the badges when collapsed.
 * @param expandedBadgeSize The size of the badges when expanded.
 * @param color The primary color for the badge icons and borders.
 * @param bgColor The background color of the badges.
 */
@SuppressLint("ModifierParameter")
@Composable
fun AmenityBadgeSet(
    amenities: Set<TrainAmenity>,
    isExpanded: Boolean,
    onSetExpanded: (Boolean) -> Unit,
    windowInsets: WindowInsets,
    contentModifier: Modifier = Modifier,
    containerModifier: Modifier = Modifier,
    collapsedBadgeSize: Dp = 15.dp,
    expandedBadgeSize: Dp = 30.dp,
    color: Color = LocalContentColor.current,
) = AmenityBadgeSetBase(
    amenities,
    isExpanded,
    onSetExpanded,
    windowInsets = windowInsets,
    contentModifier = contentModifier,
    containerModifier = containerModifier,
    onModify = null,
    collapsedBadgeSize = collapsedBadgeSize,
    expandedBadgeSize = expandedBadgeSize,
    color,
)

/**
 * Whether the UI state should allow the user to delete or add an amenity.
 * This combined type illustrates that only one should be allowed at a time.
 */
private sealed interface PreparingForModification {
    data class Delete(val amenity: TrainAmenity): PreparingForModification
    data object Add: PreparingForModification
}

@SuppressLint("ModifierParameter")
@Composable
private fun AmenityBadgeSetBase(
    amenities: Set<TrainAmenity>,
    isExpanded: Boolean,
    onSetExpanded: (Boolean) -> Unit,
    windowInsets: WindowInsets,
    contentModifier: Modifier = Modifier,
    containerModifier: Modifier = Modifier,
    onModify: ((Set<TrainAmenity>) -> Unit)? = null,
    collapsedBadgeSize: Dp = 15.dp,
    expandedBadgeSize: Dp = 30.dp,
    color: Color = LocalContentColor.current,
) {
    if (amenities.isEmpty()) return Text(
        "No amenities",
        color = color,
        fontStyle = Italic,
        modifier = contentModifier
            .padding(vertical = 5.dp)
            .clickable(null, null) { onSetExpanded(!isExpanded) },
    )

    val isModifiable = (onModify != null)
    /** Single source of truth for next operation allowed by this composable. */
    var preparingForModification by
        remember(
            isExpanded, // Reset to null when collapsed
            isModifiable,
            amenities.size
        ) { mutableStateOf<PreparingForModification?>(null) }

    val gap = (-1 * (1 - .7f) * collapsedBadgeSize / 2)

    ExpandableBadgeSet(
        isExpanded,
        onSetExpanded,
        collapsedBadgeSize = collapsedBadgeSize,
        expandedBadgeSize = expandedBadgeSize,
        windowInsets = windowInsets,
        contentModifier = contentModifier,
        containerModifier = containerModifier,
        collapsedGap = gap,
        expandedGap = -gap,
        keyedBadgesToLabels =
            amenities.toList()
            .map {
                if (
                    (preparingForModification as? PreparingForModification.Delete)
                    ?.amenity == it
                )   Badge.Delete(it)
                else Badge.Amenity(it)
            }
            .plus(
                when (preparingForModification) {
                    null if isModifiable
                            && TrainAmenity.entries.size != amenities.size ->
                        listOf(Badge.BeginAdd)
                    is PreparingForModification.Add ->
                        (TrainAmenity.entries.toSet() - amenities)
                        .map { Badge.Add(it) }
                    else ->
                        emptyList()
                }
            )
            .associateWith { badge: Badge ->

                val badgeComposable = @Composable {
                    badge.BadgeComposable(
                        onClick = if (!isExpanded) null else
                            when (badge) {
                                is Badge.Amenity if (isModifiable) -> { {
                                    preparingForModification =
                                        if (preparingForModification == null)
                                            PreparingForModification.Delete(
                                                badge.amenity
                                            )
                                        else null
                                } }
                                is Badge.Delete -> { {
                                    onModify!!(amenities - badge.amenity)
                                } }
                                is Badge.BeginAdd -> { {
                                    if (isExpanded) {
                                        preparingForModification =
                                            PreparingForModification.Add
                                    }
                                } }
                                is Badge.Add -> { {
                                    onModify!!(amenities + badge.amenity)
                                } }
                                else -> null
                            }
                    )
                }

                val labelComposable = @Composable {
                    Text(
                        badge.label,
                        style = MaterialTheme.typography.labelLarge,
                        softWrap = false,
                        maxLines = 1,
                        modifier = Modifier
                            // Can always assume [isExpanded == true]
                            .clickable {
                                when (badge) {
                                    is Badge.Amenity -> {
                                        preparingForModification =
                                            PreparingForModification.Delete(badge.amenity)
                                    }
                                    is Badge.Delete -> {
                                        preparingForModification = null
                                    }
                                    is Badge.BeginAdd -> {
                                        preparingForModification =
                                            PreparingForModification.Add
                                    }
                                    is Badge.Add -> {
                                        onModify!!(amenities + badge.amenity)
                                    }
                                }
                            },
                    )
                }

                badgeComposable to labelComposable

            }
            .mapKeys { it.key }
    )
}


sealed interface Badge {
    @Composable fun BadgeComposable(onClick: (() -> Unit)?, modifier: Modifier = Modifier)
    val label: String

    /** Generic badge for an amenity. */
    data class Amenity(val amenity: TrainAmenity): Badge {
        @Composable
        override fun BadgeComposable(onClick: (() -> Unit)?, modifier: Modifier)
                = Badge(
            AppIcons.Amenity(amenity),
            amenity.friendlyName,
            onClick
                ?.let { modifier.clickable { it() } }
                ?: modifier,
        )

        override val label = amenity.friendlyName
    }

    data class Delete(val amenity: TrainAmenity): Badge {
        @Composable
        override fun BadgeComposable(onClick: (() -> Unit)?, modifier: Modifier)
                = Badge(
            R.drawable.ic_close,
            "Delete",
            onClick
                ?.let { modifier.clickable { it() } }
                ?: modifier,
            White,
            Red,
            0f
        )

        override val label = "Delete ${amenity.friendlyName}?"
    }

    /** A badge that reveals not-present amenities to add. */
    data object BeginAdd: Badge {
        @Composable
        override fun BadgeComposable(onClick: (() -> Unit)?, modifier: Modifier)
                = Badge(
            R.drawable.ic_add,
            "Add",
            onClick
                ?.let { modifier.clickable { it() } }
                ?: modifier,
            White,
            Color.Gold,
            0f
        )

        override val label = "Add..."
    }

    /** A badge that, when clicked, adds a specific amenity to the set. */
    data class Add(val amenity: TrainAmenity): Badge {
        @Composable
        override fun BadgeComposable(onClick: (() -> Unit)?, modifier: Modifier)
                = Badge(
            AppIcons.Amenity(amenity),
            "Add ${amenity.friendlyName}",
            onClick
                ?.let { modifier.clickable { it() } }
                ?: modifier,
            White,
            Color.Gold,
            0f
        )

        override val label = "Add ${amenity.friendlyName}"
    }

}
