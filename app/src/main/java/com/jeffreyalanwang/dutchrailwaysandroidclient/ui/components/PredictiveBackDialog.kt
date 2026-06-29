package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components

import android.os.IBinder
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEvent.Companion.EDGE_NONE
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.PredictiveBackDialogSceneStrategy.Companion.predictiveBackDialog
import kotlinx.coroutines.CancellationException

fun DialogProperties.copy(
    dismissOnBackPress: Boolean = this.dismissOnBackPress,
    dismissOnClickOutside: Boolean = this.dismissOnClickOutside,
    securePolicy: SecureFlagPolicy = this.securePolicy,
    usePlatformDefaultWidth: Boolean = this.usePlatformDefaultWidth,
    decorFitsSystemWindows: Boolean = this.decorFitsSystemWindows,
    windowTitle: String = this.windowTitle,
    windowType: Int = this.windowType,
    windowToken: IBinder? = this.windowToken,
) = DialogProperties(
        dismissOnBackPress =  dismissOnBackPress,
        dismissOnClickOutside =  dismissOnClickOutside,
        securePolicy =  securePolicy,
        usePlatformDefaultWidth =  usePlatformDefaultWidth,
        decorFitsSystemWindows =  decorFitsSystemWindows,
        windowTitle =  windowTitle,
        windowType =  windowType,
        windowToken =  windowToken,
    )

/**
 * Current Compose library's [Dialog] overrides any back dispatchers outside it,
 * even if [DialogProperties.dismissOnBackPress] is set to `false`.
 */
@Composable
fun PredictiveBackDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties.copy(dismissOnBackPress = false), // We will override anyways, if it was true
    ) {
        val predictiveBackProgress = remember { Animatable(initialValue = 0f) }
        var swipeEdge by remember { mutableIntStateOf(EDGE_NONE) }

        PredictiveBackHandler(
            enabled = properties.dismissOnBackPress,
        ) { progress ->
            try {
                progress.collect { backEvent ->
                    predictiveBackProgress.snapTo(backEvent.progress)
                    swipeEdge = backEvent.swipeEdge
                }
                onDismissRequest() // Responsible for making the Dialog's scrim fade away
            } catch (e: CancellationException) {
                predictiveBackProgress.animateTo(0f)
            }
        }

        Box(
            Modifier.graphicsLayer {
                val transformOriginX = when (swipeEdge) {
                    NavigationEvent.EDGE_LEFT -> 1f
                    NavigationEvent.EDGE_RIGHT -> 0f
                    else -> 0.5f
                }
                alpha = 1 - predictiveBackProgress.value
                scaleX = 1 - predictiveBackProgress.value
                scaleY = 1 - predictiveBackProgress.value
                transformOrigin = TransformOrigin(transformOriginX, 0.5f)
            }
        ) {
            content()
        }
    }
}

internal class PredictiveBackDialogScene<T : Any>(
    override val key: Any,
    private val entry: NavEntry<T>,
    override val previousEntries: List<NavEntry<T>>,
    override val overlaidEntries: List<NavEntry<T>>,
    private val dialogProperties: DialogProperties,
    private val onBack: () -> Unit,
) : OverlayScene<T> {

    override val entries: List<NavEntry<T>> = listOf(entry)

    override val content: @Composable (() -> Unit) = {
        PredictiveBackDialog(onDismissRequest = onBack, properties = dialogProperties) { entry.Content() }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as PredictiveBackDialogScene<*>

        return key == other.key &&
                previousEntries == other.previousEntries &&
                overlaidEntries == other.overlaidEntries &&
                entry == other.entry &&
                dialogProperties == other.dialogProperties
    }

    override fun hashCode(): Int {
        return key.hashCode() * 31 +
                previousEntries.hashCode() * 31 +
                overlaidEntries.hashCode() * 31 +
                entry.hashCode() * 31 +
                dialogProperties.hashCode() * 31
    }

    override fun toString(): String {
        return "PredictiveBackDialogScene(key=$key, entry=$entry, previousEntries=$previousEntries, overlaidEntries=$overlaidEntries, dialogProperties=$dialogProperties)"
    }
}

/**
 * A [SceneStrategy] that displays entries that have added [predictiveBackDialog] to their
 * [NavEntry.metadata] within a [Dialog] instance.
 *
 * This strategy should always be added before any non-overlay scene strategies.
 */
class PredictiveBackDialogSceneStrategy<T : Any> : SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(
        entries: List<NavEntry<T>>
    ): Scene<T>? {
        val lastEntry = entries.lastOrNull()
        val dialogProperties = lastEntry?.metadata?.get(PB_DIALOG_KEY) as? DialogProperties
        return dialogProperties?.let { properties ->
            PredictiveBackDialogScene(
                key = lastEntry.contentKey,
                entry = lastEntry,
                previousEntries = entries.dropLast(1),
                overlaidEntries = entries.dropLast(1),
                dialogProperties = properties,
                onBack = onBack,
            )
        }
    }

    companion object {
        /**
         * Function to be called on the [NavEntry.metadata] to mark this entry as something that
         * should be displayed within a [PredictiveBackDialog].
         *
         * @param dialogProperties properties that should be passed to the containing [PredictiveBackDialog].
         */
        fun predictiveBackDialog(
            dialogProperties: DialogProperties = DialogProperties()
        ): Map<String, Any> = mapOf(PB_DIALOG_KEY to dialogProperties)

        internal const val PB_DIALOG_KEY = "pbDialog"
    }
}
