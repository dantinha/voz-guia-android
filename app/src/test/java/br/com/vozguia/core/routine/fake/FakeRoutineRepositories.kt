package br.com.vozguia.core.routine.fake

import br.com.vozguia.core.routine.data.MediaFavoriteRepository
import br.com.vozguia.core.routine.data.RoutinePreferenceRepository
import br.com.vozguia.core.routine.data.RoutineReminderRepository
import br.com.vozguia.core.routine.data.TrustedContactRepository
import br.com.vozguia.core.routine.data.UnrecognizedQueryRepository
import br.com.vozguia.core.routine.domain.MediaFavorite
import br.com.vozguia.core.routine.domain.RoutineReminder
import br.com.vozguia.core.routine.domain.TimePeriod
import br.com.vozguia.core.routine.domain.TrustedContact
import br.com.vozguia.core.routine.domain.UnrecognizedQuery

class FakeTrustedContactRepository : TrustedContactRepository {
    private val contacts = mutableListOf<TrustedContact>()
    private var nextId = 1L

    override suspend fun getAllContacts(): List<TrustedContact> = contacts.toList()

    override suspend fun getContactById(id: Long): TrustedContact? =
        contacts.find { it.id == id }

    override suspend fun findByRole(role: String): List<TrustedContact> =
        contacts.filter { it.relationshipRole.equals(role, ignoreCase = true) }

    override suspend fun insertContact(contact: TrustedContact): Long {
        val id = if (contact.id <= 0) nextId++ else contact.id
        val newContact = contact.copy(id = id)
        contacts.removeIf { it.id == id }
        contacts.add(newContact)
        return id
    }

    override suspend fun updateContact(contact: TrustedContact) {
        val index = contacts.indexOfFirst { it.id == contact.id }
        if (index != -1) {
            contacts[index] = contact
        } else {
            contacts.add(contact)
        }
    }

    override suspend fun deleteContact(contact: TrustedContact) {
        contacts.removeIf { it.id == contact.id }
    }

    override suspend fun getEmergencyContact(): TrustedContact? =
        contacts.find { it.isEmergencyContact }
}

class FakeRoutineReminderRepository : RoutineReminderRepository {
    private val reminders = mutableListOf<RoutineReminder>()
    private var nextId = 1L

    override suspend fun getAllReminders(): List<RoutineReminder> = reminders.toList()

    override suspend fun getActiveReminders(): List<RoutineReminder> =
        reminders.filter { it.isActive }

    override suspend fun getRemindersForDay(dayOfWeek: Int): List<RoutineReminder> =
        reminders.filter { it.isActive && it.daysOfWeek.contains(dayOfWeek) }

    override suspend fun insertReminder(reminder: RoutineReminder): Long {
        val id = if (reminder.id <= 0) nextId++ else reminder.id
        val newReminder = reminder.copy(id = id)
        reminders.removeIf { it.id == id }
        reminders.add(newReminder)
        return id
    }

    override suspend fun updateReminder(reminder: RoutineReminder) {
        val index = reminders.indexOfFirst { it.id == reminder.id }
        if (index != -1) {
            reminders[index] = reminder
        } else {
            reminders.add(reminder)
        }
    }

    override suspend fun markConfirmed(id: Long, confirmedAtTimestamp: Long) {
        val index = reminders.indexOfFirst { it.id == id }
        if (index != -1) {
            reminders[index] = reminders[index].copy(lastConfirmedAt = confirmedAtTimestamp)
        }
    }

    override suspend fun deleteReminder(id: Long) {
        reminders.removeIf { it.id == id }
    }
}

class FakeMediaFavoriteRepository : MediaFavoriteRepository {
    private val favorites = mutableListOf<MediaFavorite>()
    private var nextId = 1L

    override suspend fun getAllFavorites(): List<MediaFavorite> =
        favorites.sortedByDescending { it.playCount }

    override suspend fun getFavoritesForPeriod(period: TimePeriod): List<MediaFavorite> =
        favorites.filter { it.timePeriod == period }
            .sortedWith(compareByDescending<MediaFavorite> { it.isCuratedByCaregiver }.thenByDescending { it.playCount })

    override suspend fun insertFavorite(favorite: MediaFavorite): Long {
        val id = if (favorite.id <= 0) nextId++ else favorite.id
        val newFavorite = favorite.copy(id = id)
        favorites.removeIf { it.id == id }
        favorites.add(newFavorite)
        return id
    }

    override suspend fun incrementPlayCount(id: Long) {
        val index = favorites.indexOfFirst { it.id == id }
        if (index != -1) {
            favorites[index] = favorites[index].copy(playCount = favorites[index].playCount + 1)
        }
    }

    override suspend fun deleteFavorite(id: Long) {
        favorites.removeIf { it.id == id }
    }
}

class FakeUnrecognizedQueryRepository : UnrecognizedQueryRepository {
    private val queries = mutableListOf<UnrecognizedQuery>()
    private var nextId = 1L

    override suspend fun getRecentQueries(limit: Int): List<UnrecognizedQuery> =
        queries.sortedByDescending { it.timestamp }.take(limit)

    override suspend fun logQuery(transcribedText: String, timestamp: Long): Long {
        val id = nextId++
        queries.add(UnrecognizedQuery(id, transcribedText, timestamp))
        return id
    }

    override suspend fun resolveQuery(id: Long, resolvedAction: String) {
        val index = queries.indexOfFirst { it.id == id }
        if (index != -1) {
            queries[index] = queries[index].copy(resolvedAction = resolvedAction)
        }
    }

    override suspend fun clearOldQueries(beforeTimestamp: Long) {
        queries.removeIf { it.timestamp < beforeTimestamp }
    }
}

class FakeRoutinePreferenceRepository : RoutinePreferenceRepository {
    private val stringPrefs = mutableMapOf<String, String>()
    private val floatPrefs = mutableMapOf<String, Float>()

    override suspend fun getString(key: String, defaultValue: String): String =
        stringPrefs[key] ?: defaultValue

    override suspend fun setString(key: String, value: String) {
        stringPrefs[key] = value
    }

    override suspend fun getFloat(key: String, defaultValue: Float): Float =
        floatPrefs[key] ?: defaultValue

    override suspend fun setFloat(key: String, value: Float) {
        floatPrefs[key] = value
    }
}

class FakeContactHabitRepository : br.com.vozguia.core.routine.data.ContactHabitRepository {
    private val habits = mutableListOf<br.com.vozguia.core.routine.domain.ContactInteractionHabit>()
    private var nextId = 1L

    override suspend fun getAllHabits(): List<br.com.vozguia.core.routine.domain.ContactInteractionHabit> =
        habits.sortedByDescending { it.interactionCount }

    override suspend fun getTopHabitForPeriod(
        period: TimePeriod
    ): br.com.vozguia.core.routine.domain.ContactInteractionHabit? =
        habits.filter { it.timePeriod == period }
            .maxByOrNull { it.interactionCount }

    override suspend fun recordInteraction(contactName: String, period: TimePeriod, timestampMs: Long) {
        val index = habits.indexOfFirst {
            it.contactName.equals(contactName, ignoreCase = true) && it.timePeriod == period
        }
        if (index != -1) {
            val current = habits[index]
            habits[index] = current.copy(
                interactionCount = current.interactionCount + 1,
                lastInteractionAt = timestampMs
            )
        } else {
            habits.add(
                br.com.vozguia.core.routine.domain.ContactInteractionHabit(
                    id = nextId++,
                    contactName = contactName.trim(),
                    timePeriod = period,
                    interactionCount = 1,
                    lastInteractionAt = timestampMs
                )
            )
        }
    }
}
