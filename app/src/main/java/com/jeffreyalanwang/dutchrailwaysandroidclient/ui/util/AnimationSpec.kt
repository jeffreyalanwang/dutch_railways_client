package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util

import androidx.compose.animation.core.AnimationConstants.DefaultDurationMillis
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.AnimationVector3D
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.Animations
import androidx.compose.animation.core.DurationBasedAnimationSpec
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.FloatAnimationSpec
import androidx.compose.animation.core.FloatTweenSpec
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorizedAnimationSpec
import androidx.compose.animation.core.VectorizedDurationBasedAnimationSpec
import androidx.compose.animation.core.VectorizedFiniteAnimationSpec
import androidx.compose.animation.core.VectorizedFloatAnimationSpec
import androidx.compose.animation.core.VectorizedTweenSpec
import androidx.compose.animation.core.newInstance
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Rect

@Stable
fun translateByComponents(
    easingX: Easing = FastOutSlowInEasing,
    easingY: Easing = FastOutSlowInEasing,
    durationMillis: Int = DefaultDurationMillis,
    delayMillis: Int = 0,
):  FiniteAnimationSpec<Rect>
        = ComponentTranslationSpec(easingX, easingY, durationMillis)

data class ComponentTranslationSpec(
    val easingX: Easing = FastOutSlowInEasing,
    val easingY: Easing = FastOutSlowInEasing,
    val durationMillis: Int = DefaultDurationMillis,
    val delayMillis: Int = 0,
) : DurationBasedAnimationSpec<Rect> {

    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<Rect, V>
    ) = VectorizedComponentTranslationSpec(easingX, easingY, durationMillis, delayMillis)
}

// [androidx.compose.animation.core.VectorConverters.RectToVector]
//    convertToVector = { AnimationVector4D(it.left, it.top, it.right, it.bottom) },
//    convertFromVector = { Rect(it.v1, it.v2, it.v3, it.v4) },

/**
 * A [VectorizedFiniteAnimationSpec] that animates each dimension of a
 * [AnimationVector] with its own 1-dimensional [FiniteAnimationSpec].
 *
 * @param dimensionAnimationSpecs
 *      must have the same number of elements as [V].
 */
class VectorizedComponentAnimationSpec<V : AnimationVector>(
    vararg dimensionAnimationSpecs: FloatAnimationSpec
) : VectorizedFiniteAnimationSpec<V> {
    private val anims = dimensionAnimationSpecs
    private val sizeOfV = anims.size

    private lateinit var valueVector: V
    private lateinit var velocityVector: V
    private lateinit var endVelocityVector: V

    override fun getValueFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V,
    ): V {
        if (!::valueVector.isInitialized) {
            valueVector = initialValue.newInstance()
        }
        for (i in 0 until sizeOfV) {
            valueVector[i] =
                anims[i].getValueFromNanos(
                    playTimeNanos,
                    initialValue[i],
                    targetValue[i],
                    initialVelocity[i],
                )
        }
        return valueVector
    }

    override fun getVelocityFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V,
    ): V {
        if (!::velocityVector.isInitialized) {
            velocityVector = initialVelocity.newInstance()
        }
        for (i in 0 until sizeOfV) {
            velocityVector[i] =
                anims[i].getVelocityFromNanos(
                    playTimeNanos,
                    initialValue[i],
                    targetValue[i],
                    initialVelocity[i],
                )
        }
        return velocityVector
    }

    override fun getEndVelocity(initialValue: V, targetValue: V, initialVelocity: V): V {
        if (!::endVelocityVector.isInitialized) {
            endVelocityVector = initialVelocity.newInstance()
        }
        for (i in 0 until sizeOfV) {
            endVelocityVector[i] =
                anims[i].getEndVelocity(initialValue[i], targetValue[i], initialVelocity[i])
        }
        return endVelocityVector
    }

    @Suppress("MethodNameUnits")
    override fun getDurationNanos(initialValue: V, targetValue: V, initialVelocity: V): Long {
        var maxDuration = 0L
        for (i in 0 until sizeOfV) {
            maxDuration =
                maxOf(
                    maxDuration,
                    anims[i].getDurationNanos(initialValue[i], targetValue[i], initialVelocity[i]),
                )
        }
        return maxDuration
    }

    // Unfortunately we need to recreate behavior of
    // internal methods from compose animations package
    @Suppress("UNCHECKED_CAST")
    private fun V.newInstance(): V
    = when (sizeOfV) {
        1 -> AnimationVector1D(0f)
        2 -> AnimationVector2D(0f, 0f)
        3 -> AnimationVector3D(0f, 0f, 0f)
        4 -> AnimationVector4D(0f, 0f, 0f, 0f)
        else -> throw IllegalArgumentException("Unsupported size: $sizeOfV")
    } as V
}