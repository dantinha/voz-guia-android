package br.com.vozguia.core.routine.utilities

import br.com.vozguia.core.routine.data.RoutineReminderRepository
import br.com.vozguia.core.routine.domain.RoutineReminder
import java.util.Calendar

class MedicineReminderController(
    private val reminderRepository: RoutineReminderRepository
) {

    /**
     * Gera o texto falado do anúncio de alarme de remédio ou compromisso.
     */
    fun generateReminderAlert(reminder: RoutineReminder): String {
        val hourPart = reminder.timeHhMm.split(":").firstOrNull() ?: "o horário"
        return "Atenção: são $hourPart horas. Hora de ${reminder.title}. Aperte o botão de volume para confirmar que ouviu."
    }

    /**
     * Confirma o recebimento do lembrete ao clique do botão físico de volume.
     */
    suspend fun confirmReminder(reminderId: Long, confirmedAt: Long = System.currentTimeMillis()): String {
        val all = reminderRepository.getAllReminders()
        val reminder = all.find { it.id == reminderId }
        reminderRepository.markConfirmed(reminderId, confirmedAt)

        val title = reminder?.title ?: "compromisso"
        return "Lembrete confirmado: $title."
    }

    /**
     * Localiza lembretes ativos configurados para o horário atual.
     */
    suspend fun getDueReminders(calendar: Calendar = Calendar.getInstance()): List<RoutineReminder> {
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val currentTimeHhMm = "%02d:%02d".format(hour, minute)

        val activeReminders = reminderRepository.getRemindersForDay(dayOfWeek)
        return activeReminders.filter { it.timeHhMm == currentTimeHhMm }
    }
}
