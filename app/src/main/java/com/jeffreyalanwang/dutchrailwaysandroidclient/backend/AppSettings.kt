package com.jeffreyalanwang.dutchrailwaysandroidclient.backend
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.asStateWithInitialValueOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

@Composable
fun AppSettingsProvider(
    value: SettingsViewModel = viewModel<SettingsViewModelImpl>(factory = SettingsViewModelImpl.Factory),
    content: @Composable () -> Unit,
) = CompositionLocalProvider(LocalAppSettings provides value, content)

val LocalAppSettings = staticCompositionLocalOf<SettingsViewModel> { SettingsViewModelDummy() }

abstract class SettingsViewModel: ViewModel() {
    abstract fun update(transform: (Settings) -> Settings)

    protected abstract val stateFlow: StateFlow<Settings>

    val state
        @Composable get() = stateFlow.collectAsState()

    @Composable
    fun <T> stateOf(selector: (Settings) -> T): State<T> {
        val mappedInitial = remember { selector(stateFlow.value) }
        val mappedFlow = remember { stateFlow.map { selector(it) } }
        return mappedFlow.collectAsState(mappedInitial)
    }
}

class SettingsViewModelDummy: SettingsViewModel() {
    private val field = MutableStateFlow(Settings.dataStoreDefaults)

    override val stateFlow = field.asStateFlow()
    override fun update(transform: (Settings) -> Settings) = field.update(transform)
}

class SettingsViewModelImpl
private constructor(
    private val dataStore: DataStore<Settings>,
): SettingsViewModel() {
    // This wrapper is incorporated because it provides a [ViewModelScope].
    // Without this, we would need to use a UI-bound [CoroutineScope] to
    // call [DataStore.updateData].
    //
    // This class is defined here, not the `viewmodel` directory,
    // because it does not correspond to a specific screen or composable in the UI.

    override val stateFlow = dataStore.data
        .asStateWithInitialValueOf(Settings.whileLoadingDefaults)

    override fun update(
        transform: (Settings) -> Settings,
    ) {
        viewModelScope.launch {
            dataStore.updateData(transform)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val dataStore = get(APPLICATION_KEY)!!.settingsDataStore
                SettingsViewModelImpl(dataStore)
            }
        }
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
                isEditAccessLocked = true,
            )
        val whileLoadingDefaults =
            Settings(
                isEditAccessLocked = true,
            )
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