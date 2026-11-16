package br.com.vozguia.core.data.repository

/**
 * Repositório para rastreamento de quotas de uso da API em nuvem (Free Tier).
 */
interface QuotaRepository {
    suspend fun getTodayCloudCallCount(): Int
    suspend fun getLastCloudCallTimestamp(): Long
    suspend fun recordCloudCall(timestampMs: Long = System.currentTimeMillis())
    suspend fun resetDailyCount()
}
