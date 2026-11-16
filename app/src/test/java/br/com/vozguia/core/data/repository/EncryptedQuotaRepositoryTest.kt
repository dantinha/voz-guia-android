package br.com.vozguia.core.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class EncryptedQuotaRepositoryTest {

    private lateinit var fakeStorage: FakeQuotaStorage
    private var currentDay = "2026-11-10"
    private lateinit var repository: PersistentQuotaRepository

    @Before
    fun setUp() {
        fakeStorage = FakeQuotaStorage()
        currentDay = "2026-11-10"
        repository = PersistentQuotaRepository(
            storage = fakeStorage,
            dateKeyProvider = { currentDay }
        )
    }

    @Test
    fun `inicia contador zerado e incrementa a cada chamada em nuvem registrada`() = runTest {
        assertThat(repository.getTodayCloudCallCount()).isEqualTo(0)

        repository.recordCloudCall(timestampMs = 1_000L)
        repository.recordCloudCall(timestampMs = 5_000L)

        assertThat(repository.getTodayCloudCallCount()).isEqualTo(2)
        assertThat(repository.getLastCloudCallTimestamp()).isEqualTo(5_000L)
    }

    @Test
    fun `zera contador automaticamente ao virar o dia mantendo timestamp anterior`() = runTest {
        repository.recordCloudCall(timestampMs = 10_000L)
        repository.recordCloudCall(timestampMs = 20_000L)
        assertThat(repository.getTodayCloudCallCount()).isEqualTo(2)

        // Virada de meia-noite para o dia seguinte
        currentDay = "2026-11-11"

        assertThat(repository.getTodayCloudCallCount()).isEqualTo(0)
        repository.recordCloudCall(timestampMs = 90_000L)
        assertThat(repository.getTodayCloudCallCount()).isEqualTo(1)
    }

    @Test
    fun `resetDailyCount limpa contador manualmente`() = runTest {
        repository.recordCloudCall(timestampMs = 1_000L)
        assertThat(repository.getTodayCloudCallCount()).isEqualTo(1)

        repository.resetDailyCount()
        assertThat(repository.getTodayCloudCallCount()).isEqualTo(0)
    }

    private class FakeQuotaStorage : QuotaStorage {
        private val ints = mutableMapOf<String, Int>()
        private val longs = mutableMapOf<String, Long>()
        private val strings = mutableMapOf<String, String>()

        override fun getInt(key: String, defaultValue: Int): Int = ints[key] ?: defaultValue
        override fun putInt(key: String, value: Int) { ints[key] = value }
        override fun getLong(key: String, defaultValue: Long): Long = longs[key] ?: defaultValue
        override fun putLong(key: String, value: Long) { longs[key] = value }
        override fun getString(key: String, defaultValue: String): String = strings[key] ?: defaultValue
        override fun putString(key: String, value: String) { strings[key] = value }
    }
}
