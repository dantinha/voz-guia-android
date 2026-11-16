package br.com.vozguia.core.caregiver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MotorolaPermissionItem(
    val title: String,
    val description: String,
    val isGranted: Boolean,
    val settingsActionIntent: String
)

interface MotorolaPermissionProvider {
    fun getPermissionsStatus(): List<MotorolaPermissionItem>
}

data class CaregiverDashboardUiState(
    val permissions: List<MotorolaPermissionItem> = emptyList(),
    val contacts: List<TrustedContact> = emptyList(),
    val reminders: List<RoutineReminder> = emptyList(),
    val favorites: List<MediaFavorite> = emptyList(),
    val unrecognizedQueries: List<UnrecognizedQuery> = emptyList(),
    val ttsSpeed: Float = 0.92f,
    val userFirstName: String = "Valdir",
    val isLoading: Boolean = false
)

class CaregiverDashboardViewModel(
    private val contactRepository: TrustedContactRepository,
    private val reminderRepository: RoutineReminderRepository,
    private val favoriteRepository: MediaFavoriteRepository,
    private val queryRepository: UnrecognizedQueryRepository,
    private val preferenceRepository: RoutinePreferenceRepository,
    private val permissionProvider: MotorolaPermissionProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaregiverDashboardUiState())
    val uiState: StateFlow<CaregiverDashboardUiState> = _uiState.asStateFlow()

    init {
        refreshState()
    }

    fun refreshState() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val permissions = permissionProvider.getPermissionsStatus()
            val contacts = contactRepository.getAllContacts()
            val reminders = reminderRepository.getAllReminders()
            val favorites = favoriteRepository.getAllFavorites()
            val queries = queryRepository.getRecentQueries()
            val ttsSpeed = preferenceRepository.getFloat("pref_tts_speed", 0.92f)
            val userName = preferenceRepository.getString("pref_user_first_name", "Valdir")

            _uiState.value = CaregiverDashboardUiState(
                permissions = permissions,
                contacts = contacts,
                reminders = reminders,
                favorites = favorites,
                unrecognizedQueries = queries,
                ttsSpeed = ttsSpeed,
                userFirstName = userName,
                isLoading = false
            )
        }
    }

    fun addContact(
        name: String,
        phone: String,
        role: String,
        aliases: List<String>,
        isEmergency: Boolean = false
    ) {
        viewModelScope.launch {
            val contact = TrustedContact(
                displayName = name.trim(),
                phoneNumber = phone.trim(),
                relationshipRole = role.trim().lowercase(),
                phoneticAliases = aliases.map { it.trim().lowercase() },
                isVipFamily = true,
                isEmergencyContact = isEmergency
            )
            contactRepository.insertContact(contact)
            refreshState()
        }
    }

    fun deleteContact(contact: TrustedContact) {
        viewModelScope.launch {
            contactRepository.deleteContact(contact)
            refreshState()
        }
    }

    fun addReminder(
        title: String,
        timeHhMm: String,
        daysOfWeek: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7)
    ) {
        viewModelScope.launch {
            val reminder = RoutineReminder(
                title = title.trim(),
                timeHhMm = timeHhMm.trim(),
                daysOfWeek = daysOfWeek,
                isActive = true
            )
            reminderRepository.insertReminder(reminder)
            refreshState()
        }
    }

    fun deleteReminder(reminderId: Long) {
        viewModelScope.launch {
            reminderRepository.deleteReminder(reminderId)
            refreshState()
        }
    }

    fun addFavorite(
        title: String,
        searchQuery: String,
        timePeriod: TimePeriod
    ) {
        viewModelScope.launch {
            val fav = MediaFavorite(
                title = title.trim(),
                searchQuery = searchQuery.trim(),
                timePeriod = timePeriod,
                isCuratedByCaregiver = true
            )
            favoriteRepository.insertFavorite(fav)
            refreshState()
        }
    }

    fun deleteFavorite(favoriteId: Long) {
        viewModelScope.launch {
            favoriteRepository.deleteFavorite(favoriteId)
            refreshState()
        }
    }

    fun setTtsSpeed(speed: Float) {
        viewModelScope.launch {
            val clamped = speed.coerceIn(0.75f, 1.20f)
            preferenceRepository.setFloat("pref_tts_speed", clamped)
            _uiState.value = _uiState.value.copy(ttsSpeed = clamped)
        }
    }

    fun getTtsPreviewText(): String {
        return "Esta é a velocidade de fala que seu pai ouvirá no dia a dia com o Voz Guia."
    }

    fun linkUnrecognizedQueryAsContactAlias(
        queryId: Long,
        contactId: Long,
        aliasText: String
    ) {
        viewModelScope.launch {
            val contact = contactRepository.getContactById(contactId)
            if (contact != null) {
                val updatedAliases = contact.phoneticAliases.toMutableList()
                val cleanAlias = aliasText.trim().lowercase()
                if (!updatedAliases.contains(cleanAlias)) {
                    updatedAliases.add(cleanAlias)
                }
                val updated = contact.copy(phoneticAliases = updatedAliases)
                contactRepository.updateContact(updated)
                queryRepository.resolveQuery(queryId, "Vinculado como apelido de ${contact.displayName}")
                refreshState()
            }
        }
    }
}
