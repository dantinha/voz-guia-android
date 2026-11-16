package br.com.vozguia.core.routine.briefing

import br.com.vozguia.core.routine.domain.RoutineReminder
import br.com.vozguia.core.routine.fake.FakeRoutinePreferenceRepository
import br.com.vozguia.core.routine.fake.FakeRoutineReminderRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class MorningBriefingOrchestratorTest {

    private lateinit var preferenceRepo: FakeRoutinePreferenceRepository
    private lateinit var reminderRepo: FakeRoutineReminderRepository
    private lateinit var orchestrator: MorningBriefingOrchestrator

    @Before
    fun setup() = runTest {
        preferenceRepo = FakeRoutinePreferenceRepository()
        reminderRepo = FakeRoutineReminderRepository()

        preferenceRepo.setString(MorningBriefingOrchestrator.KEY_USER_NAME, "Valdir")
        reminderRepo.insertReminder(
            RoutineReminder(
                id = 1L,
                title = "remédio da pressão",
                timeHhMm = "08:00",
                daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
                isActive = true
            )
        )

        orchestrator = MorningBriefingOrchestrator(
            preferenceRepository = preferenceRepo,
            reminderRepository = reminderRepo
        )
    }

    @Test
    fun `identifies morning briefing is due on first wakeup after 6 AM`() = runTest {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 30)
            set(Calendar.DAY_OF_MONTH, 9)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.YEAR, 2026)
        }

        val shouldBrief = orchestrator.shouldTriggerBriefing(calendar)
        assertThat(shouldBrief).isTrue()
    }

    @Test
    fun `does not trigger morning briefing before 6 AM`() = runTest {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 5)
            set(Calendar.MINUTE, 45)
        }

        val shouldBrief = orchestrator.shouldTriggerBriefing(calendar)
        assertThat(shouldBrief).isFalse()
    }

    @Test
    fun `does not trigger twice on the same day`() = runTest {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.DAY_OF_MONTH, 9)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.YEAR, 2026)
        }

        // Deliver first briefing
        orchestrator.deliverBriefing(calendar, batteryPercent = 85, unreadMessageSummary = null)

        // Try checking again
        val shouldBriefSecondTime = orchestrator.shouldTriggerBriefing(calendar)
        assertThat(shouldBriefSecondTime).isFalse()
    }

    @Test
    fun `generates warm spoken briefing with name, date, battery, reminders and unread messages`() = runTest {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.DAY_OF_MONTH, 9)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.YEAR, 2026)
        }

        val speech = orchestrator.deliverBriefing(
            calendar = calendar,
            batteryPercent = 85,
            unreadMessageSummary = "Gabriel mandou 1 mensagem de áudio"
        )

        assertThat(speech).contains("Bom dia, seu Valdir!")
        assertThat(speech).contains("segunda-feira, dia 9 de novembro")
        assertThat(speech).contains("A bateria está em 85%")
        assertThat(speech).contains("Lembrete: remédio da pressão às 08:00")
        assertThat(speech).contains("Gabriel mandou 1 mensagem de áudio")
        assertThat(speech).contains("O que o senhor gostaria de fazer agora?")
    }
}
