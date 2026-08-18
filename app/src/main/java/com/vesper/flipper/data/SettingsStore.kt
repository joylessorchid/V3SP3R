package com.vesper.flipper.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.vesper.flipper.security.EncryptedStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vesper_settings")

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // OpenRouter API Key.
    //
    // The key is held in EncryptedSharedPreferences (AES-256-GCM under a Keystore
    // master key), NOT in the DataStore alongside ordinary settings — DataStore
    // writes plaintext protobuf, so a key stored there is readable by anyone with
    // file-level access to the device (root, forensic extraction, an unlocked
    // bootloader). Everything else in this class is non-sensitive and stays put.
    //
    // LEGACY_API_KEY is the old plaintext location. It is read once, copied into
    // encrypted storage and then removed, so an existing install stops leaving the
    // key in the clear rather than merely storing a second copy correctly.
    private val LEGACY_API_KEY = stringPreferencesKey("openrouter_api_key")

    private val encryptedStorage by lazy { EncryptedStorage(context) }

    /** Bumped on every write so collectors of [apiKey] re-read. */
    private val apiKeyRevision = MutableStateFlow(0)

    val apiKey: Flow<String?> = apiKeyRevision.map { readApiKey() }

    private suspend fun readApiKey(): String? = withContext(Dispatchers.IO) {
        val legacy = context.dataStore.data.first()[LEGACY_API_KEY]
        if (!legacy.isNullOrBlank()) {
            // Commit the encrypted copy and confirm it reads back before deleting the
            // plaintext. A hard-kill between an async write and the plaintext removal
            // could otherwise drop the key entirely, forcing the user to re-enter it.
            val committed = encryptedStorage.putStringSync(SECURE_API_KEY, legacy)
            if (committed && encryptedStorage.getString(SECURE_API_KEY) == legacy) {
                context.dataStore.edit { it.remove(LEGACY_API_KEY) }
            }
            return@withContext legacy
        }
        encryptedStorage.getString(SECURE_API_KEY)
    }

    suspend fun setApiKey(key: String) {
        withContext(Dispatchers.IO) {
            // Durable write first, then drop any stale plaintext copy from an older
            // build — never the other way round.
            encryptedStorage.putStringSync(SECURE_API_KEY, key)
            context.dataStore.edit { it.remove(LEGACY_API_KEY) }
        }
        apiKeyRevision.value += 1
    }

    // Selected Model
    private val SELECTED_MODEL = stringPreferencesKey("selected_model")

    val selectedModel: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SELECTED_MODEL] ?: DEFAULT_MODEL
    }

    suspend fun setSelectedModel(model: String) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_MODEL] = model
        }
    }

    // Last Connected Device
    private val LAST_DEVICE_ADDRESS = stringPreferencesKey("last_device_address")
    private val LAST_DEVICE_NAME = stringPreferencesKey("last_device_name")
    private val LAST_CHAT_SESSION_ID = stringPreferencesKey("last_chat_session_id")

    val lastDeviceAddress: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[LAST_DEVICE_ADDRESS]
    }

    val lastDeviceName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[LAST_DEVICE_NAME]
    }

    suspend fun setLastDevice(address: String, name: String) {
        context.dataStore.edit { preferences ->
            preferences[LAST_DEVICE_ADDRESS] = address
            preferences[LAST_DEVICE_NAME] = name
        }
    }

    val lastChatSessionId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[LAST_CHAT_SESSION_ID]
    }

    suspend fun setLastChatSessionId(sessionId: String?) {
        context.dataStore.edit { preferences ->
            if (sessionId.isNullOrBlank()) {
                preferences.remove(LAST_CHAT_SESSION_ID)
            } else {
                preferences[LAST_CHAT_SESSION_ID] = sessionId
            }
        }
    }

    // Auto-connect setting
    private val AUTO_CONNECT = booleanPreferencesKey("auto_connect")

    val autoConnect: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_CONNECT] ?: true
    }

    suspend fun setAutoConnect(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_CONNECT] = enabled
        }
    }

    // Default project path on Flipper
    private val DEFAULT_PROJECT_PATH = stringPreferencesKey("default_project_path")

    val defaultProjectPath: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[DEFAULT_PROJECT_PATH] ?: "/ext/apps_data/vesper"
    }

    suspend fun setDefaultProjectPath(path: String) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_PROJECT_PATH] = path
        }
    }

    // Permission auto-grant duration (milliseconds)
    private val PERMISSION_DURATION = longPreferencesKey("permission_duration")

    val permissionDuration: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[PERMISSION_DURATION] ?: 15 * 60 * 1000L // 15 minutes default
    }

    suspend fun setPermissionDuration(durationMs: Long) {
        context.dataStore.edit { preferences ->
            preferences[PERMISSION_DURATION] = durationMs
        }
    }

    // Auto-approve by risk tier
    private val AUTO_APPROVE_MEDIUM = booleanPreferencesKey("auto_approve_medium")
    private val AUTO_APPROVE_HIGH = booleanPreferencesKey("auto_approve_high")

    val autoApproveMedium: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_APPROVE_MEDIUM] ?: false
    }

    suspend fun setAutoApproveMedium(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_APPROVE_MEDIUM] = enabled
        }
    }

    val autoApproveHigh: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_APPROVE_HIGH] ?: false
    }

    suspend fun setAutoApproveHigh(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_APPROVE_HIGH] = enabled
        }
    }

    // Haptic feedback
    private val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")

    val hapticFeedback: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HAPTIC_FEEDBACK] ?: true
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HAPTIC_FEEDBACK] = enabled
        }
    }

    // Dark mode
    private val DARK_MODE = booleanPreferencesKey("dark_mode")

    val darkMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DARK_MODE] ?: true
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE] = enabled
        }
    }

    // Audit log retention (days)
    private val AUDIT_RETENTION_DAYS = intPreferencesKey("audit_retention_days")

    val auditRetentionDays: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[AUDIT_RETENTION_DAYS] ?: 30
    }

    suspend fun setAuditRetentionDays(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[AUDIT_RETENTION_DAYS] = days
        }
    }

    // AI agent max model/tool loop iterations
    private val AI_MAX_ITERATIONS = intPreferencesKey("ai_max_iterations")

    val aiMaxIterations: Flow<Int> = context.dataStore.data.map { preferences ->
        (preferences[AI_MAX_ITERATIONS] ?: DEFAULT_AI_MAX_ITERATIONS)
            .coerceIn(MIN_AI_MAX_ITERATIONS, MAX_AI_MAX_ITERATIONS)
    }

    suspend fun setAiMaxIterations(value: Int) {
        context.dataStore.edit { preferences ->
            preferences[AI_MAX_ITERATIONS] = value.coerceIn(MIN_AI_MAX_ITERATIONS, MAX_AI_MAX_ITERATIONS)
        }
    }

    // TTS (routed through OpenRouter — no separate key needed)
    private val TTS_ENABLED = booleanPreferencesKey("tts_enabled")
    private val TTS_VOICE_ID = stringPreferencesKey("tts_voice_id")
    private val TTS_AUTO_SPEAK = booleanPreferencesKey("tts_auto_speak")

    val ttsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[TTS_ENABLED] ?: false
    }

    suspend fun setTtsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[TTS_ENABLED] = enabled
        }
    }

    val ttsVoiceId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[TTS_VOICE_ID] ?: DEFAULT_TTS_VOICE
    }

    suspend fun setTtsVoiceId(voiceId: String) {
        context.dataStore.edit { preferences ->
            preferences[TTS_VOICE_ID] = voiceId
        }
    }

    val ttsAutoSpeak: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[TTS_AUTO_SPEAK] ?: false
    }

    suspend fun setTtsAutoSpeak(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[TTS_AUTO_SPEAK] = enabled
        }
    }

    // Smart Glasses Bridge
    private val GLASSES_ENABLED = booleanPreferencesKey("glasses_enabled")
    private val GLASSES_BRIDGE_URL = stringPreferencesKey("glasses_bridge_url")
    private val GLASSES_AUTO_SEND = booleanPreferencesKey("glasses_auto_send")
    private val GLASSES_AUTO_CONNECT = booleanPreferencesKey("glasses_auto_connect")
    private val GLASSES_SAILOR_MOUTH = booleanPreferencesKey("glasses_sailor_mouth")
    private val GLASSES_MUTED = booleanPreferencesKey("glasses_muted")

    val glassesEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[GLASSES_ENABLED] ?: false
    }

    suspend fun setGlassesEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[GLASSES_ENABLED] = enabled
        }
    }

    val glassesBridgeUrl: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[GLASSES_BRIDGE_URL]
    }

    suspend fun setGlassesBridgeUrl(url: String?) {
        context.dataStore.edit { preferences ->
            if (url.isNullOrBlank()) {
                preferences.remove(GLASSES_BRIDGE_URL)
            } else {
                preferences[GLASSES_BRIDGE_URL] = url
            }
        }
    }

    val glassesAutoSend: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[GLASSES_AUTO_SEND] ?: true
    }

    suspend fun setGlassesAutoSend(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[GLASSES_AUTO_SEND] = enabled
        }
    }

    val glassesAutoConnect: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[GLASSES_AUTO_CONNECT] ?: false
    }

    suspend fun setGlassesAutoConnect(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[GLASSES_AUTO_CONNECT] = enabled
        }
    }

    val glassesSailorMouth: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[GLASSES_SAILOR_MOUTH] ?: false
    }

    suspend fun setGlassesSailorMouth(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[GLASSES_SAILOR_MOUTH] = enabled
        }
    }

    val glassesMuted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[GLASSES_MUTED] ?: false
    }

    suspend fun setGlassesMuted(muted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[GLASSES_MUTED] = muted
        }
    }

    companion object {
        /** Entry name inside EncryptedSharedPreferences holding the OpenRouter key. */
        private const val SECURE_API_KEY = "openrouter_api_key"

        // Chosen for what this app asks of a model rather than for price. The previous
        // default here was google/gemini-3.7-flash, picked because it is cheap and fast
        // — and it returned an empty response to "scan wifi networks", which is not an
        // edge case for a Flipper Zero controller but the ordinary work. Google's models
        // decline a large share of security tooling.
        //
        // A default that refuses the app's primary use case is not a default, whatever
        // it costs per token. Gemini is still one tap away in Settings for anyone whose
        // usage is mostly file browsing and IR remotes.
        const val DEFAULT_MODEL = "anthropic/claude-sonnet-5"
        // Shimmer: soft, warm female — default TTS voice (OpenAI via OpenRouter)
        const val DEFAULT_TTS_VOICE = "shimmer"
        const val DEFAULT_AI_MAX_ITERATIONS = 10
        const val MIN_AI_MAX_ITERATIONS = 4
        const val MAX_AI_MAX_ITERATIONS = 20

        // Used when fetching the live catalog fails (offline / rate-limited), and — via
        // OpenRouterModelCatalog's putIfAbsent — to fill in any manufacturer the live
        // catalog did not return. That second use is why stale entries here are not
        // harmless: they get injected into the picker the user is choosing from.
        //
        // Keep this list one-per-manufacturer and in the same order as
        // OpenRouterModelCatalog.MAJOR_MANUFACTURERS, and keep both aligned with what
        // OpenRouter actually serves. Every id below was checked against the live
        // catalogue; all twelve of the ids this replaces had been withdrawn.
        val FALLBACK_MODELS = listOf(
            ModelInfo("google/gemini-3.7-flash", "Gemini 3.7 Flash", "Latest Google"),
            ModelInfo("anthropic/claude-sonnet-5", "Claude Sonnet 5", "Latest Anthropic"),
            ModelInfo("openai/gpt-5.6-sol", "GPT-5.6 Sol", "Latest OpenAI"),
            ModelInfo("x-ai/grok-4.6", "Grok 4.6", "Latest xAI"),
            ModelInfo("qwen/qwen3.8-max", "Qwen3.8 Max", "Latest Qwen"),
            ModelInfo("deepseek/deepseek-v4-pro", "DeepSeek V4 Pro", "Latest DeepSeek"),
            ModelInfo("moonshotai/kimi-k3", "Kimi K3", "Latest Moonshot"),
            ModelInfo("meta/muse-spark-1.2", "Muse Spark 1.2", "Latest Meta"),
            ModelInfo("minimax/minimax-m3", "MiniMax M3", "Latest MiniMax")
        )

        fun getModelDisplayName(
            modelId: String,
            availableModels: List<ModelInfo> = FALLBACK_MODELS
        ): String {
            return availableModels.find { it.id == modelId }?.displayName ?: modelId
        }
    }
}

/**
 * Model information for display in settings
 */
data class ModelInfo(
    val id: String,           // OpenRouter model ID
    val displayName: String,  // User-friendly name
    val description: String   // Short description
)
