package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import com.jeffreyalanwang.dutchrailwaysandroidclient.map
import com.jeffreyalanwang.dutchrailwaysandroidclient.plusInsert
import kotlin.math.max

/**
 * A box that holds a button on potentially either side of the content.
 *
 * If a button is only present on the left or right, the other side
 * receives enough padding to visually balance the content.
 */
@Composable
fun MarginButtonsBox(
    modifier: Modifier = Modifier,
    left: @Composable (() -> Unit)? = null,
    right: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) = Layout(
        content = { left?.invoke(); content(); right?.invoke() },
        modifier = modifier,
    ) { measurables, constraints ->
        val (leftM, contentM, rightM) = measurables
            .let {
                left?.let { _ -> it }
                    ?: it.plusInsert(0, null)
            }
            .let {
                right?.let { _ -> it }
                    ?: it.plus(null)
            }
            .run { Triple(this[0], this[1]!!, this[2]) }

        // Use [content] to determine box's height
        val height = contentM
            .maxIntrinsicHeight(constraints.maxWidth)
            .let { constraints.constrainHeight(it) }

        val (leftP, rightP) = (leftM to rightM).map {
            it?.measure( Constraints(maxHeight = height) )
        }

        val (leftW, rightW) =
            if (leftP != null && rightP != null) {
                leftP.width to rightP.width

            // Provide visual balance via padding if only one side has content
            } else if (leftP != null) {
                leftP.width.let { it to (it/2) }
            } else if (rightP != null) {
                rightP.width.let { (it/2) to it }

            } else {
                0 to 0
            }

        val contentP = contentM.measure(
            Constraints(
                maxWidth = max(0, constraints.maxWidth - leftW - rightW),
                maxHeight = height,
            )
        )

        layout(
            width = leftW + contentP.width + rightW,
            height = height
        ) {
            leftP?.place(0, 0)
            contentP.place(leftW, 0)
            rightP?.place(leftW + contentP.width, 0)
        }
    }