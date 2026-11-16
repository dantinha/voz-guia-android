package br.com.vozguia

import android.app.Application
import androidx.room.Room
import br.com.vozguia.core.data.repository.EncryptedPrefsQuotaStorage
import br.com.vozguia.core.data.repository.PersistentQuotaRepository
import br.com.vozguia.core.data.repository.QuotaRepository
import br.com.vozguia.core.guardrails.GuardrailPipeline
import br.com.vozguia.core.guardrails.VozGuiaGuardrailFactory
import br.com.vozguia.core.routine.data.RoomContactHabitRepository
import br.com.vozguia.core.routine.data.RoomMediaFavoriteRepository
import br.com.vozguia.core.routine.data.RoomRoutineReminderRepository
import br.com.vozguia.core.routine.data.RoomTrustedContactRepository
import br.com.vozguia.core.routine.data.RoomUnrecognizedQueryRepository
import br.com.vozguia.core.routine.data.SharedPreferencesRoutinePreferenceRepository
import br.com.vozguia.core.routine.data.room.VozGuiaDatabase

class VozGuiaApplication : Application() {

    val database: VozGuiaDatabase by lazy {
        Room.databaseBuilder(this, VozGuiaDatabase::class.java, "vozguia.db")
            .addMigrations(VozGuiaDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()
    }

    val contactRepository by lazy { RoomTrustedContactRepository(database.contactDao()) }
    val reminderRepository by lazy { RoomRoutineReminderRepository(database.reminderDao()) }
    val favoriteRepository by lazy { RoomMediaFavoriteRepository(database.favoriteDao()) }
    val queryRepository by lazy { RoomUnrecognizedQueryRepository(database.unrecognizedQueryDao()) }
    val contactHabitRepository by lazy { RoomContactHabitRepository(database.contactHabitDao()) }
    val preferenceRepository by lazy { SharedPreferencesRoutinePreferenceRepository(this) }

    val quotaRepository: QuotaRepository by lazy {
        PersistentQuotaRepository(EncryptedPrefsQuotaStorage(this))
    }

    val guardrailPipeline: GuardrailPipeline by lazy {
        VozGuiaGuardrailFactory.createDefaultPipeline(quotaRepository)
    }
}
