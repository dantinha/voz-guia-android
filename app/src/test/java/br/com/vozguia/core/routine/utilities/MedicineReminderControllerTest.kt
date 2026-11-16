package br.com.vozguia.core.routine.utilities

import br.com.vozguia.core.routine.domain.RoutineReminder
import br.com.vozguia.core.routine.fake.FakeRoutineReminderRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class MedicineReminderControllerTest {

    private lateinit var reminderRepository: FakeRoutineReminderRepository
    private lateinit var controller: MedicineReminderController

    private val bloodPressureReminder = RoutineReminder(
        id = 1L,
        title = "Tomar o remédio da pressão",
        timeHhMm = "08:00",
        daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
        isActive = true
    )

    @Before
    fun setup() = runTest {
        reminderRepository = FakeRoutineReminderRepository()
        reminderRepository.insertReminder(bloodPressureReminder)
        controller = MedicineReminderController(reminderRepository)
    }

    @Test
    fun `generates spoken alert prompt for matching reminder`() = runTest {
        val alert = controller.generateReminderAlert(bloodPressureReminder)
        assertThat(alert).contains("Atenção: são 08 horas")
        assertThat(alert).contains("Hora de Tomar o remédio da pressão")
        assertThat(alert).contains("Aperte o botão de volume para confirmar que ouviu")
    }

    @Test
    fun `marks reminder as confirmed when user clicks volume button`() = runTest {
        val timestamp = 1731150000000L
        val confirmationResponse = controller.confirmReminder(
            reminderId = 1L,
            confirmedAt = timestamp
        )

        assertThat(confirmationResponse).contains("Lembrete confirmado")
        assertThat(confirmationResponse).contains("Tomar o remédio da pressão")

        // Check repository updated
        val reminders = reminderRepository.getAllReminders()
        val updated = reminders.find { it.id == 1L }
        assertThat(updated?.lastConfirmedAt).isEqualTo(timestamp)
    }

    @Test
    fun `finds active reminders due for given time`() = runTest {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
        }
        val dueReminders = controller.getDueReminders(calendar)
        assertThat(dueReminders).hasSize(1)
        assertThat(dueReminders.first().title).isEqualTo("Tomar o remédio da pressão")
    }
}
