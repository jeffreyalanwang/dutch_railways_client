package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import com.jeffreyalanwang.dutchrailwaysandroidclient.R
import kotlin.math.min

@Preview(widthDp = 300, heightDp = 300)
@Composable
private fun IconWithBadgePreview() {
    IconWithBadge(
        badge = painterResource(R.drawable.ic_close),
    ) {
        Icon(
            painterResource(R.drawable.ic_dr_trainservice),
            contentDescription = null,
        )
    }
}

@Composable
fun IconWithBadge(
    badge: Painter,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    BadgeLayout(modifier) {
        icon()
        Icon(badge, null, Modifier.badge(2/3f, .87f))
    }
}

private data class BadgeParentData(
    var sizeRatio: Float,
    var overlapRatio: Float,
)

@Immutable
private data class BadgeModifierElement(
    var sizeRatio: Float,
    var overlapRatio: Float,
) : ModifierNodeElement<BadgeModifierNode>() {
    override fun create() = BadgeModifierNode(sizeRatio, overlapRatio)
    override fun update(node: BadgeModifierNode) {
        node.sizeRatio = sizeRatio
        node.overlapRatio = overlapRatio
    }
}

private class BadgeModifierNode(
    var sizeRatio: Float,
    var overlapRatio: Float,
) : ParentDataModifierNode, Modifier.Node() {
    override fun Density.modifyParentData(parentData: Any?): Any {
        return BadgeParentData(sizeRatio, overlapRatio)
    }
}

@Immutable
object BadgeLayoutScope {
    @Stable
    fun Modifier.badge(
        sizeRatio: Float = 1/2f,
        overlapRatio: Float = 1/2f,
    ): Modifier
        = this.then(BadgeModifierElement(sizeRatio, overlapRatio))
}

/**
 * @param sizeRatio: ratio of badge width to icon width.
 * @param overlapRatio: portion of badge width to cover icon width.
 */
@Composable
private fun BadgeLayout(
    modifier: Modifier = Modifier,
    sizeRatio: Float = 2/3f,
    overlapRatio: Float = .87f,
    content: @Composable BadgeLayoutScope.() -> Unit,
) = Layout(
        modifier = modifier,
        content = { BadgeLayoutScope.content() },
    ) { measurables, constraints ->
        require(measurables.size == 2)
        val (iconMeasurable, badgeMeasurable) =
            measurables
            .indexOfFirst { (it.parentData is BadgeParentData) }
            .also { require(it >= 0) }
            .let { badgeIndex ->
                measurables[if (badgeIndex == 0) 1 else 0] to
                measurables[badgeIndex]
            }

        fun iconSize(totalSize: Int): Float {
            // badge non-overlapped size is [sizeRatio * iconSize * (1 - overlapRatio)]
            // total size is [iconSize + 2 * badgeNonoverlapSize]
            // total size is [iconSize + 2 * sizeRatio * iconSize * (1 - overlapRatio)]

            // total size is [iconSize * (1 + 2 * sizeRatio * (1 - overlapRatio))]

            return totalSize / (1 + 2 * sizeRatio * (1 - overlapRatio))
        }

        val minIconSize = iconSize(
            min(constraints.minWidth, constraints.minHeight)
        )
        val maxIconSize = iconSize(
            min(constraints.minWidth, constraints.maxHeight)
        )

        val iconPlaceable = iconMeasurable.measure(
            Constraints(
                minWidth = minIconSize.toInt(),
                minHeight = minIconSize.toInt(),

                maxWidth = maxIconSize.toInt(),
                maxHeight = maxIconSize.toInt(),
            )
        )

        val badgePlaceable = badgeMeasurable.measure(
            Constraints(
                minWidth = (iconPlaceable.width * sizeRatio).toInt(),
                maxWidth = (iconPlaceable.width * sizeRatio).toInt(),
                minHeight = (iconPlaceable.height * sizeRatio).toInt(),
                maxHeight = (iconPlaceable.height * sizeRatio).toInt(),
            )
        )

        layout(
            width = iconPlaceable.width + (2 * badgePlaceable.width * (1 - overlapRatio)).toInt(),
            height = iconPlaceable.height + (2 * badgePlaceable.height * (1 - overlapRatio)).toInt(),
        ) {
            iconPlaceable.place(
                (badgePlaceable.width * (1 - overlapRatio)).toInt(),
                (badgePlaceable.height * (1 - overlapRatio)).toInt(),
            )
            badgePlaceable.place(
                iconPlaceable.width + (badgePlaceable.width * (1 - 2 * overlapRatio)).toInt(),
                iconPlaceable.height + (badgePlaceable.height * (1 - 2 * overlapRatio)).toInt(),
            )
        }
    }