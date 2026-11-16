package br.com.vozguia.core.routine.data

import android.content.Context
import android.content.SharedPreferences
import br.com.vozguia.core.routine.data.room.MediaFavoriteDao
import br.com.vozguia.core.routine.data.room.MediaFavoriteEntity
import br.com.vozguia.core.routine.data.room.RoutineReminderDao
import br.com.vozguia.core.routine.data.room.RoutineReminderEntity
import br.com.vozguia.core.routine.data.room.TrustedContactDao
import br.com.vozguia.core.routine.data.room.TrustedContactEntity
import br.com.vozguia.core.routine.data.room.UnrecognizedQueryDao
import br.com.vozguia.core.routine.data.room.UnrecognizedQueryEntity
import br.com.vozguia.core.routine.domain.MediaFavorite
import br.com.vozguia.core.routine.domain.RoutineReminder
import br.com.vozguia.core.routine.domain.TimePeriod
import br.com.vozguia.core.routine.domain.TrustedContact
import br.com.vozguia.core.routine.domain.UnrecognizedQuery

class RoomTrustedContactRepository(
    private val dao: TrustedContactDao
) : TrustedContactRepository {
    override suspend fun getAllContacts(): List<TrustedContact> =
        dao.getAll().map { it.toDomain() }

    override suspend fun getContactById(id: Long): TrustedContact? =
        dao.getById(id)?.toDomain()

    override suspend fun findByRole(role: String): List<TrustedContact> =
        dao.findByRole(role).map { it.toDomain() }

    override suspend fun insertContact(contact: TrustedContact): Long =
        dao.insert(TrustedContactEntity.fromDomain(contact))

    override suspend fun updateContact(contact: TrustedContact) =
        dao.update(TrustedContactEntity.fromDomain(contact))

    override suspend fun deleteContact(contact: TrustedContact) =
        dao.delete(TrustedContactEntity.fromDomain(contact))

    override suspend fun getEmergencyContact(): TrustedContact? =
        dao.getEmergencyContact()?.toDomain()
}

class RoomRoutineReminderRepository(
    private val dao: RoutineReminderDao
) : RoutineReminderRepository {
    override suspend fun getAllReminders(): List<RoutineReminder> =
        dao.getAll().map { it.toDomain() }

    override suspend fun getActiveReminders(): List<RoutineReminder> =
        dao.getActive().map { it.toDomain() }

    override suspend fun getRemindersForDay(dayOfWeek: Int): List<RoutineReminder> =
        dao.getActive().map { it.toDomain() }.filter { it.daysOfWeek.contains(dayOfWeek) }

    override suspend fun insertReminder(reminder: RoutineReminder): Long =
        dao.insert(RoutineReminderEntity.fromDomain(reminder))

    override suspend fun updateReminder(reminder: RoutineReminder) =
        dao.update(RoutineReminderEntity.fromDomain(reminder))

    override suspend fun markConfirmed(id: Long, confirmedAtTimestamp: Long) =
        dao.markConfirmed(id, confirmedAtTimestamp)

    override suspend fun deleteReminder(id: Long) =
        dao.delete(id)
}

class RoomMediaFavoriteRepository(
    private val dao: MediaFavoriteDao
) : MediaFavoriteRepository {
    override suspend fun getAllFavorites(): List<MediaFavorite> =
        dao.getAll().map { it.toDomain() }

    override suspend fun getFavoritesForPeriod(period: TimePeriod): List<MediaFavorite> =
        dao.getForPeriod(period.name).map { it.toDomain() }

    override suspend fun insertFavorite(favorite: MediaFavorite): Long =
        dao.insert(MediaFavoriteEntity.fromDomain(favorite))

    override suspend fun incrementPlayCount(id: Long) =
        dao.incrementPlayCount(id)

    override suspend fun deleteFavorite(id: Long) =
        dao.delete(id)
}

class RoomUnrecognizedQueryRepository(
    private val dao: UnrecognizedQueryDao
) : UnrecognizedQueryRepository {
    override suspend fun getRecentQueries(limit: Int): List<UnrecognizedQuery> =
        dao.getRecent(limit).map { it.toDomain() }

    override suspend fun logQuery(transcribedText: String, timestamp: Long): Long =
        dao.insert(UnrecognizedQueryEntity(transcribedText = transcribedText, timestamp = timestamp, resolvedAction = null))

    override suspend fun resolveQuery(id: Long, resolvedAction: String) =
        dao.resolve(id, resolvedAction)

    override suspend fun clearOldQueries(beforeTimestamp: Long) =
        dao.clearOld(beforeTimestamp)
}

class SharedPreferencesRoutinePreferenceRepository(
    context: Context
) : RoutinePreferenceRepository {
    private val prefs: SharedPreferences = context.getSharedPreferences("vozguia_routine_prefs", Context.MODE_PRIVATE)

    override suspend fun getString(key: String, defaultValue: String): String =
        prefs.getString(key, defaultValue) ?: defaultValue

    override suspend fun setString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override suspend fun getFloat(key: String, defaultValue: Float): Float =
        prefs.getFloat(key, defaultValue)

    override suspend fun setFloat(key: String, value: Float) {
        prefs.edit().putFloat(key, value).apply()
    }
}
