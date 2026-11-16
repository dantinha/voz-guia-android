package br.com.vozguia.core.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Abstração simples de armazenamento chave-valor para permitir testes unitários puros na JVM.
 */
interface QuotaStorage {
    fun getInt(key: String, defaultValue: Int): Int
    fun putInt(key: String, value: Int)
    fun getLong(key: String, defaultValue: Long): Long
    fun putLong(key: String, value: Long)
    fun getString(key: String, defaultValue: String): String
    fun putString(key: String, value: String)
}

/**
 * Implementação persistente de rastreamento de cotas do Free Tier com virada automática à meia-noite.
 */
class PersistentQuotaRepository(
    private val storage: QuotaStorage,
    private val dateKeyProvider: () -> String = {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
) : QuotaRepository {

    companion object {
        private const val KEY_DATE = "quota_date"
        private const val KEY_COUNT = "quota_count"
        private const val KEY_LAST_TS = "quota_last_ts"
    }

    private fun ensureCurrentDay() {
        val today = dateKeyProvider()
        val storedDate = storage.getString(KEY_DATE, "")
        if (storedDate != today) {
            storage.putString(KEY_DATE, today)
            storage.putInt(KEY_COUNT, 0)
        }
    }

    override suspend fun getTodayCloudCallCount(): Int {
        ensureCurrentDay()
        return storage.getInt(KEY_COUNT, 0)
    }

    override suspend fun getLastCloudCallTimestamp(): Long {
        return storage.getLong(KEY_LAST_TS, 0L)
    }

    override suspend fun recordCloudCall(timestampMs: Long) {
        ensureCurrentDay()
        val nextCount = storage.getInt(KEY_COUNT, 0) + 1
        storage.putInt(KEY_COUNT, nextCount)
        storage.putLong(KEY_LAST_TS, timestampMs)
    }

    override suspend fun resetDailyCount() {
        storage.putString(KEY_DATE, dateKeyProvider())
        storage.putInt(KEY_COUNT, 0)
    }
}

/**
 * Armazenamento criptografado via AndroidX Security Crypto (Keystore hardware-backed no Edge 50 Neo).
 */
class EncryptedPrefsQuotaStorage(context: Context) : QuotaStorage {

    private val prefs: SharedPreferences by lazy {
        val appContext = context.applicationContext
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            appContext,
            "vozguia_cloud_quota_enc",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun getInt(key: String, defaultValue: Int): Int = prefs.getInt(key, defaultValue)
    override fun putInt(key: String, value: Int) { prefs.edit().putInt(key, value).apply() }
    override fun getLong(key: String, defaultValue: Long): Long = prefs.getLong(key, defaultValue)
    override fun putLong(key: String, value: Long) { prefs.edit().putLong(key, value).apply() }
    override fun getString(key: String, defaultValue: String): String = prefs.getString(key, defaultValue) ?: defaultValue
    override fun putString(key: String, value: String) { prefs.edit().putString(key, value).apply() }
}
