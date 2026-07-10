package com.jeffreyalanwang.dutchrailwaysandroidclient.backend
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.asStateWithInitialValueOf
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

class SettingsViewModel(
    private val dataStore: DataStore<Settings>,
): ViewModel() {
    // This wrapper is incorporated because it provides a [ViewModelScope].
    // Without this, we would need to use a UI-bound [CoroutineScope] to
    // call [DataStore.updateData].
    //
    // This class is defined here, not the `viewmodel` directory,
    // because it does not correspond to a specific screen or composable in the UI.

    private val flow = dataStore.data
        .asStateWithInitialValueOf(Settings.whileLoadingDefaults)

    val value
        @Composable get() = flow.collectAsState()

    fun set(
        transform: (Settings) -> Settings,
    ) = viewModelScope.launch {
        context.settingsDataStore.updateData(transform)
    }
}

val Context.settingsDataStore by dataStore(
    "settings.json",
    SettingsSerializer,
    corruptionHandler = ReplaceFileCorruptionHandler { Settings.dataStoreDefaults },
)

@Serializable
data class Settings(
    val isEditAccessLocked: Boolean,
) {
    companion object {
        val dataStoreDefaults =
            Settings(
                isEditAccessLocked = false,
            )
        val whileLoadingDefaults = dataStoreDefaults
    }
}

object SettingsSerializer : Serializer<Settings> {

    override val defaultValue = Settings.dataStoreDefaults

    override suspend fun readFrom(input: InputStream): Settings =
        try {
            input.readBytes().decodeToString().let { Json.decodeFromString(it) }
        } catch (serialization: SerializationException) {
            throw CorruptionException("Unable to read Settings", serialization)
        }

    override suspend fun writeTo(
        t: Settings,
        output: OutputStream
    ) {
        t.let { Json.encodeToString(it) }.encodeToByteArray()
            .let { output.write(it) }
    }

}