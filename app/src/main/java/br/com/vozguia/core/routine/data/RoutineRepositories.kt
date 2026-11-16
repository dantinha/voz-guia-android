package br.com.vozguia.core.routine.data

import br.com.vozguia.core.routine.domain.MediaFavorite
import br.com.vozguia.core.routine.domain.RoutineReminder
import br.com.vozguia.core.routine.domain.TimePeriod
import br.com.vozguia.core.routine.domain.TrustedContact
import br.com.vozguia.core.routine.domain.UnrecognizedQuery

/**
 * Contrato de persistência para contatos familiares confiáveis.
 */
interface TrustedContactRepository {
    suspend fun getAllContacts(): List<TrustedContact>
    suspend fun getContactById(id: Long): TrustedContact?
    suspend fun findByRole(role: String): List<TrustedContact>
    suspend fun insertContact(contact: TrustedContact): Long
    suspend fun updateContact(contact: TrustedContact)
    suspend fun deleteContact(contact: TrustedContact)
    suspend fun getEmergencyContact(): TrustedContact?
}

/**
 * Contrato de persistência para lembretes de rotina e remédios.
 */
interface RoutineReminderRepository {
    suspend fun getAllReminders(): List<RoutineReminder>
    suspend fun getActiveReminders(): List<RoutineReminder>
    suspend fun getRemindersForDay(dayOfWeek: Int): List<RoutineReminder>
    suspend fun insertReminder(reminder: RoutineReminder): Long
    suspend fun updateReminder(reminder: RoutineReminder)
    suspend fun markConfirmed(id: Long, confirmedAtTimestamp: Long)
    suspend fun deleteReminder(id: Long)
}

/**
 * Contrato de persistência para favoritos de rádio e YouTube.
 */
interface MediaFavoriteRepository {
    suspend fun getAllFavorites(): List<MediaFavorite>
    suspend fun getFavoritesForPeriod(period: TimePeriod): List<MediaFavorite>
    suspend fun insertFavorite(favorite: MediaFavorite): Long
    suspend fun incrementPlayCount(id: Long)
    suspend fun deleteFavorite(id: Long)
}

/**
 * Contrato de persistência para observabilidade local de comandos não compreendidos.
 */
interface UnrecognizedQueryRepository {
    suspend fun getRecentQueries(limit: Int = 20): List<UnrecognizedQuery>
    suspend fun logQuery(transcribedText: String, timestamp: Long): Long
    suspend fun resolveQuery(id: Long, resolvedAction: String)
    suspend fun clearOldQueries(beforeTimestamp: Long)
}

/**
 * Contrato de persistência de preferências de rotina e do cuidador.
 */
interface RoutinePreferenceRepository {
    suspend fun getString(key: String, defaultValue: String): String
    suspend fun setString(key: String, value: String)
    suspend fun getFloat(key: String, defaultValue: Float): Float
    suspend fun setFloat(key: String, value: Float)
}
