package br.com.vozguia.core.routine.data.room

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import br.com.vozguia.core.routine.domain.MediaFavorite
import br.com.vozguia.core.routine.domain.RoutineReminder
import br.com.vozguia.core.routine.domain.TimePeriod
import br.com.vozguia.core.routine.domain.TrustedContact
import br.com.vozguia.core.routine.domain.UnrecognizedQuery

@Entity(tableName = "trusted_contacts")
data class TrustedContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val phoneNumber: String,
    val whatsappJid: String?,
    val relationshipRole: String,
    val phoneticAliases: List<String>,
    val isVipFamily: Boolean,
    val isEmergencyContact: Boolean
) {
    fun toDomain(): TrustedContact = TrustedContact(
        id = id,
        displayName = displayName,
        phoneNumber = phoneNumber,
        whatsappJid = whatsappJid,
        relationshipRole = relationshipRole,
        phoneticAliases = phoneticAliases,
        isVipFamily = isVipFamily,
        isEmergencyContact = isEmergencyContact
    )

    companion object {
        fun fromDomain(domain: TrustedContact): TrustedContactEntity = TrustedContactEntity(
            id = domain.id,
            displayName = domain.displayName,
            phoneNumber = domain.phoneNumber,
            whatsappJid = domain.whatsappJid,
            relationshipRole = domain.relationshipRole,
            phoneticAliases = domain.phoneticAliases,
            isVipFamily = domain.isVipFamily,
            isEmergencyContact = domain.isEmergencyContact
        )
    }
}

@Entity(tableName = "routine_reminders")
data class RoutineReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val timeHhMm: String,
    val daysOfWeek: List<Int>,
    val isActive: Boolean,
    val lastConfirmedAt: Long?
) {
    fun toDomain(): RoutineReminder = RoutineReminder(
        id = id,
        title = title,
        timeHhMm = timeHhMm,
        daysOfWeek = daysOfWeek,
        isActive = isActive,
        lastConfirmedAt = lastConfirmedAt
    )

    companion object {
        fun fromDomain(domain: RoutineReminder): RoutineReminderEntity = RoutineReminderEntity(
            id = domain.id,
            title = domain.title,
            timeHhMm = domain.timeHhMm,
            daysOfWeek = domain.daysOfWeek,
            isActive = domain.isActive,
            lastConfirmedAt = domain.lastConfirmedAt
        )
    }
}

@Entity(tableName = "media_favorites")
data class MediaFavoriteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val searchQuery: String,
    val timePeriod: String, // "MANHA", "TARDE", "NOITE"
    val playCount: Int,
    val isCuratedByCaregiver: Boolean
) {
    fun toDomain(): MediaFavorite = MediaFavorite(
        id = id,
        title = title,
        searchQuery = searchQuery,
        timePeriod = TimePeriod.valueOf(timePeriod),
        playCount = playCount,
        isCuratedByCaregiver = isCuratedByCaregiver
    )

    companion object {
        fun fromDomain(domain: MediaFavorite): MediaFavoriteEntity = MediaFavoriteEntity(
            id = domain.id,
            title = domain.title,
            searchQuery = domain.searchQuery,
            timePeriod = domain.timePeriod.name,
            playCount = domain.playCount,
            isCuratedByCaregiver = domain.isCuratedByCaregiver
        )
    }
}

@Entity(tableName = "unrecognized_queries")
data class UnrecognizedQueryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transcribedText: String,
    val timestamp: Long,
    val resolvedAction: String?
) {
    fun toDomain(): UnrecognizedQuery = UnrecognizedQuery(
        id = id,
        transcribedText = transcribedText,
        timestamp = timestamp,
        resolvedAction = resolvedAction
    )

    companion object {
        fun fromDomain(domain: UnrecognizedQuery): UnrecognizedQueryEntity = UnrecognizedQueryEntity(
            id = domain.id,
            transcribedText = domain.transcribedText,
            timestamp = domain.timestamp,
            resolvedAction = domain.resolvedAction
        )
    }
}

class RoutineTypeConverters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String =
        value?.joinToString(";;") ?: ""

    @TypeConverter
    fun toStringList(value: String?): List<String> =
        if (value.isNullOrBlank()) emptyList() else value.split(";;")

    @TypeConverter
    fun fromIntList(value: List<Int>?): String =
        value?.joinToString(",") ?: ""

    @TypeConverter
    fun toIntList(value: String?): List<Int> =
        if (value.isNullOrBlank()) emptyList() else value.split(",").mapNotNull { it.trim().toIntOrNull() }
}

@Dao
interface TrustedContactDao {
    @Query("SELECT * FROM trusted_contacts ORDER BY displayName ASC")
    suspend fun getAll(): List<TrustedContactEntity>

    @Query("SELECT * FROM trusted_contacts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TrustedContactEntity?

    @Query("SELECT * FROM trusted_contacts WHERE relationshipRole = :role")
    suspend fun findByRole(role: String): List<TrustedContactEntity>

    @Query("SELECT * FROM trusted_contacts WHERE isEmergencyContact = 1 LIMIT 1")
    suspend fun getEmergencyContact(): TrustedContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: TrustedContactEntity): Long

    @Update
    suspend fun update(contact: TrustedContactEntity)

    @Delete
    suspend fun delete(contact: TrustedContactEntity)
}

@Dao
interface RoutineReminderDao {
    @Query("SELECT * FROM routine_reminders WHERE isActive = 1 ORDER BY timeHhMm ASC")
    suspend fun getActive(): List<RoutineReminderEntity>

    @Query("SELECT * FROM routine_reminders ORDER BY timeHhMm ASC")
    suspend fun getAll(): List<RoutineReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: RoutineReminderEntity): Long

    @Update
    suspend fun update(reminder: RoutineReminderEntity)

    @Query("UPDATE routine_reminders SET lastConfirmedAt = :timestamp WHERE id = :id")
    suspend fun markConfirmed(id: Long, timestamp: Long)

    @Query("DELETE FROM routine_reminders WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface MediaFavoriteDao {
    @Query("SELECT * FROM media_favorites ORDER BY playCount DESC")
    suspend fun getAll(): List<MediaFavoriteEntity>

    @Query("SELECT * FROM media_favorites WHERE timePeriod = :period ORDER BY isCuratedByCaregiver DESC, playCount DESC")
    suspend fun getForPeriod(period: String): List<MediaFavoriteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: MediaFavoriteEntity): Long

    @Query("UPDATE media_favorites SET playCount = playCount + 1 WHERE id = :id")
    suspend fun incrementPlayCount(id: Long)

    @Query("DELETE FROM media_favorites WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface UnrecognizedQueryDao {
    @Query("SELECT * FROM unrecognized_queries ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<UnrecognizedQueryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(query: UnrecognizedQueryEntity): Long

    @Query("UPDATE unrecognized_queries SET resolvedAction = :resolvedAction WHERE id = :id")
    suspend fun resolve(id: Long, resolvedAction: String)

    @Query("DELETE FROM unrecognized_queries WHERE timestamp < :beforeTimestamp")
    suspend fun clearOld(beforeTimestamp: Long)
}

@Database(
    entities = [
        TrustedContactEntity::class,
        RoutineReminderEntity::class,
        MediaFavoriteEntity::class,
        UnrecognizedQueryEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoutineTypeConverters::class)
abstract class VozGuiaDatabase : RoomDatabase() {
    abstract fun contactDao(): TrustedContactDao
    abstract fun reminderDao(): RoutineReminderDao
    abstract fun favoriteDao(): MediaFavoriteDao
    abstract fun unrecognizedQueryDao(): UnrecognizedQueryDao
}
