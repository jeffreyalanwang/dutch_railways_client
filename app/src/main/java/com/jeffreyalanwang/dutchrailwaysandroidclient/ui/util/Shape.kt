package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import com.jeffreyalanwang.dutchrailwaysandroidclient.compareTo
import com.jeffreyalanwang.dutchrailwaysandroidclient.letIf
import com.jeffreyalanwang.dutchrailwaysandroidclient.plusInsert

fun Morph.shapeAt(progress: Float)
  = MorphPolygonShape(
        morph = this,
        progress = progress,
    )

/**
 * @author https://developer.android.com/develop/ui/compose/graphics/draw/shapes#morph-button
 */
class MorphPolygonShape(
    val morph: Morph,
    val progress: Float,
) : Shape {
    private val matrix = Matrix()

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        // Below assumes that you haven't changed the default radius of 1f,
        //      nor the centerX and centerY of 0f.
        // By default this stretches the path to the size of the container,
        //      if you don't want stretching, use the same size.width for
        //      both x and y.
        matrix.scale(size.width / 2f, size.height / 2f)
        matrix.translate(1f, 1f)

        val path = morph.toPath(progress).asComposePath()
        path.transform(matrix)
        return Outline.Generic(path)
    }
}

@JvmInline
value class MultiMorph
private constructor(
    /**
     * Keys represent the exclusive maximum progress value for the
     * corresponding value [Morph].
     *
     * Ranges must be continuous, sorted in ascending order,
     * and span from 0<=..<1.
     */
    private val morphs: List<Pair<OpenEndRange<Float>, Morph>>,
) {
    companion object {
        fun multiMorphOf(
            shapes: List<Pair<Float, RoundedPolygon>>
        ) = MultiMorph(
            shapes
                .letIf({ it.first().first != 0f }) {
                    it.plusInsert(0, 0f to it.first().second)
                }
                .letIf({ it.last().first != 1f }) {
                    it.plus(1f to it.last().second)
                }
                .zipWithNext { a, b ->
                    val range = a.first..<b.first
                    val morph = Morph(a.second, b.second)
                    range to morph
                }
        )

        fun multiMorphOf(vararg shapes: Pair<Float, RoundedPolygon>) = multiMorphOf(shapes.toList())
    }

    /**
     * @return  The morph to be used at the provided progress point,
     *          and the progress of the individual morph at that point.
     */
    fun fromTotalProgress(progress: Float): Pair<Float, Morph> {
        if (progress <= 0f) {
            return 0f to morphs.first().second
        }
        if (progress >= 1f) { // this one is required because [morphs] ranges are open-ended
            return 1f to morphs.last().second
        }
        // We are now guaranteed that 0f < progress < 1f

        val (range, morph) = morphs.run {
            val index = binarySearch { it.first.compareTo(progress) }
            morphs[index]
        }
        val subProgress = range.run {
            // [ start <= progress < end ], therefore, [ 0 <= subProgress < 1 ].
            // (We return a progress value of 1 using the guard at the top of this method.)
            (progress - start) / (endExclusive - start)
        }

        return subProgress to morph
    }

    fun shapeAt(progress: Float): Shape {
        val (subProgress, subMorph) = fromTotalProgress(progress)
        return MorphPolygonShape(
            morph = subMorph,
            progress = subProgress
        )
    }
}