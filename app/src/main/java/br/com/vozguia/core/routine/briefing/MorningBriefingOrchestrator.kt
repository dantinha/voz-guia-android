package br.com.vozguia.core.routine.briefing

import br.com.vozguia.core.routine.data.RoutinePreferenceRepository
import br.com.vozguia.core.routine.data.RoutineReminderRepository
import br.com.vozguia.core.routine.utilities.DailyAutonomyUtilities
import java.util.Calendar

class MorningBriefingOrchestrator(
    private val preferenceRepository: RoutinePreferenceRepository,
    private val reminderRepository: RoutineReminderRepository,
    private val utilities: DailyAutonomyUtilities = DailyAutonomyUtilities()
) {

    companion object {
        const val KEY_LAST_BRIEFING_DATE = "pref_last_morning_briefing_date"
        const val KEY_USER_NAME = "pref_user_first_name"
    }

    /**
     * Verifica se o briefing matinal deve ser acionado neste momento.
     * Condições: Hora entre 06:00 e 11:59 e data diferente da última em que foi entregue.
     */
    suspend fun shouldTriggerBriefing(calendar: Calendar = Calendar.getInstance()): Boolean {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        if (hour !in 6..11) return false

        val todayDate = toDateString(calendar)
        val lastDate = preferenceRepository.getString(KEY_LAST_BRIEFING_DATE, "")
        return todayDate != lastDate
    }

    /**
     * Monta o texto falado do briefing matinal acolhedor e registra a data atual.
     */
    suspend fun deliverBriefing(
        calendar: Calendar = Calendar.getInstance(),
        batteryPercent: Int,
        unreadMessageSummary: String? = null
    ): String {
        val userName = preferenceRepository.getString(KEY_USER_NAME, "amigo")
        val dateSpoken = utilities.formatDateSpoken(calendar)
        val batterySpoken = utilities.formatBatterySpoken(batteryPercent, isCharging = false)

        // Lembretes do dia
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val remindersToday = reminderRepository.getRemindersForDay(dayOfWeek)
        val remindersPart = if (remindersToday.isNotEmpty()) {
            val remindersList = remindersToday.joinToString(", ") { "${it.title} às ${it.timeHhMm}" }
            "Lembrete: $remindersList."
        } else {
            ""
        }

        val messagesPart = if (!unreadMessageSummary.isNullOrBlank()) {
            "$unreadMessageSummary."
        } else {
            ""
        }

        // Registra que já foi entregue hoje
        val todayDate = toDateString(calendar)
        preferenceRepository.setString(KEY_LAST_BRIEFING_DATE, todayDate)

        val greeting = if (userName.isNotBlank()) "Bom dia, seu $userName!" else "Bom dia!"

        val parts = listOf(
            greeting,
            "Hoje é $dateSpoken.",
            "$batterySpoken.",
            remindersPart,
            messagesPart,
            "O que o senhor gostaria de fazer agora?"
        ).filter { it.isNotBlank() }

        return parts.joinToString(" ")
    }

    private fun toDateString(calendar: Calendar): String {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return "%04d-%02d-%02d".format(year, month, day)
    }
}
