package br.com.vozguia.core.caregiver

import br.com.vozguia.core.routine.domain.MediaFavorite
import br.com.vozguia.core.routine.domain.RoutineReminder
import br.com.vozguia.core.routine.domain.TimePeriod
import br.com.vozguia.core.routine.domain.TrustedContact
import br.com.vozguia.core.routine.fake.FakeMediaFavoriteRepository
import br.com.vozguia.core.routine.fake.FakeRoutinePreferenceRepository
import br.com.vozguia.core.routine.fake.FakeRoutineReminderRepository
import br.com.vozguia.core.routine.fake.FakeTrustedContactRepository
import br.com.vozguia.core.routine.fake.FakeUnrecognizedQueryRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CaregiverDashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var contactRepo: FakeTrustedContactRepository
    private lateinit var reminderRepo: FakeRoutineReminderRepository
    private lateinit var favoriteRepo: FakeMediaFavoriteRepository
    private lateinit var queryRepo: FakeUnrecognizedQueryRepository
    private lateinit var prefRepo: FakeRoutinePreferenceRepository
    private lateinit var permissionProvider: FakeMotorolaPermissionProvider
    private lateinit var viewModel: CaregiverDashboardViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        contactRepo = FakeTrustedContactRepository()
        reminderRepo = FakeRoutineReminderRepository()
        favoriteRepo = FakeMediaFavoriteRepository()
        queryRepo = FakeUnrecognizedQueryRepository()
        prefRepo = FakeRoutinePreferenceRepository()
        permissionProvider = FakeMotorolaPermissionProvider()

        viewModel = CaregiverDashboardViewModel(
            contactRepository = contactRepo,
            reminderRepository = reminderRepo,
            favoriteRepository = favoriteRepo,
            queryRepository = queryRepo,
            preferenceRepository = prefRepo,
            permissionProvider = permissionProvider
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads initial dashboard state with permissions checklist`() = runTest(testDispatcher) {
        viewModel.refreshState()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.permissions).hasSize(6)
        assertThat(state.permissions.all { it.isGranted }).isFalse()
    }

    @Test
    fun `adds new family contact and updates state`() = runTest(testDispatcher) {
        viewModel.addContact(
            name = "Gabriel Silva",
            phone = "11999998888",
            role = "filho",
            aliases = listOf("biel", "meu filho"),
            isEmergency = true
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val contacts = viewModel.uiState.value.contacts
        assertThat(contacts).hasSize(1)
        assertThat(contacts.first().displayName).isEqualTo("Gabriel Silva")
        assertThat(contacts.first().isEmergencyContact).isTrue()
    }

    @Test
    fun `adds medicine reminder and updates state`() = runTest(testDispatcher) {
        viewModel.addReminder(
            title = "Remédio da pressão",
            timeHhMm = "08:00",
            daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val reminders = viewModel.uiState.value.reminders
        assertThat(reminders).hasSize(1)
        assertThat(reminders.first().title).isEqualTo("Remédio da pressão")
    }

    @Test
    fun `adjusts voice speed preference and validates preview text`() = runTest(testDispatcher) {
        viewModel.setTtsSpeed(0.92f)
        testDispatcher.scheduler.advanceUntilIdle()

        val speed = viewModel.uiState.value.ttsSpeed
        assertThat(speed).isEqualTo(0.92f)
        val preview = viewModel.getTtsPreviewText()
        assertThat(preview).contains("Esta é a velocidade de fala que seu pai ouvirá")
    }

    @Test
    fun `links unrecognized query directly to an existing contact as an alias`() = runTest(testDispatcher) {
        // Setup contact and unrecognized query
        val contactId = contactRepo.insertContact(
            TrustedContact(
                id = 1L,
                displayName = "Gabriel Silva",
                phoneNumber = "11999998888",
                relationshipRole = "filho",
                phoneticAliases = listOf("gabriel")
            )
        )
        val queryId = queryRepo.logQuery("bielzão", 1731150000000L)

        viewModel.linkUnrecognizedQueryAsContactAlias(
            queryId = queryId,
            contactId = contactId,
            aliasText = "bielzão"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedContact = contactRepo.getContactById(contactId)
        assertThat(updatedContact?.phoneticAliases).contains("bielzão")

        val queries = queryRepo.getRecentQueries()
        assertThat(queries.first().resolvedAction).contains("Gabriel Silva")
    }
}

class FakeMotorolaPermissionProvider : MotorolaPermissionProvider {
    var accessibility = false
    var notificationListener = false
    var phone = false
    var audio = false
    var overlay = false
    var batteryOptimization = false

    override fun getPermissionsStatus(): List<MotorolaPermissionItem> = listOf(
        MotorolaPermissionItem("Acessibilidade", "Leitura estruturada de tela", accessibility, "action_accessibility"),
        MotorolaPermissionItem("Notificações", "Captura de mensagens do WhatsApp", notificationListener, "action_notification"),
        MotorolaPermissionItem("Telefone", "Realizar chamadas diretas no viva-voz", phone, "action_phone"),
        MotorolaPermissionItem("Microfone", "Reconhecimento de fala offline", audio, "action_audio"),
        MotorolaPermissionItem("Sobreposição de Tela", "Exibir feedback e tela protetora", overlay, "action_overlay"),
        MotorolaPermissionItem("Bateria Irrestrita", "Blindagem contra fechamento em background", batteryOptimization, "action_battery")
    )
}
