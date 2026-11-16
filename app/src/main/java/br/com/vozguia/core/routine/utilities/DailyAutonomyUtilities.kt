package br.com.vozguia.core.routine.utilities

import java.util.Calendar

class DailyAutonomyUtilities {

    private val daysOfWeekPt = arrayOf(
        "", // Calendar.SUNDAY is 1
        "domingo",
        "segunda-feira",
        "terça-feira",
        "quarta-feira",
        "quinta-feira",
        "sexta-feira",
        "sábado"
    )

    private val monthsPt = arrayOf(
        "janeiro", "fevereiro", "março", "abril", "maio", "junho",
        "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"
    )

    /**
     * Converte o horário em fala acolhedora e natural em português.
     */
    fun formatTimeSpoken(calendar: Calendar = Calendar.getInstance()): String {
        val hour24 = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        if (hour24 == 0 && minute == 0) return "É meia-noite"
        if (hour24 == 12 && minute == 0) return "É meio-dia"

        val period = when (hour24) {
            in 0..5 -> "da madrugada"
            in 6..11 -> "da manhã"
            in 12..17 -> "da tarde"
            else -> "da noite"
        }

        val hour12 = when (hour24) {
            0 -> 12
            in 1..12 -> hour24
            else -> hour24 - 12
        }

        val hourPrefix = when {
            hour24 == 0 -> "São 12 horas"
            hour24 == 12 -> "É meio-dia"
            hour12 == 1 -> "É uma hora"
            else -> "São $hour12 horas"
        }

        val minutePart = when (minute) {
            0 -> ""
            1 -> " e 1 minuto"
            else -> " e $minute minutos"
        }

        return "$hourPrefix$minutePart $period".trim()
    }

    /**
     * Converte a data em formato falado natural.
     */
    fun formatDateSpoken(calendar: Calendar = Calendar.getInstance()): String {
        val dayOfWeekIndex = calendar.get(Calendar.DAY_OF_WEEK)
        val dayOfWeek = daysOfWeekPt.getOrElse(dayOfWeekIndex) { "" }
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val month = monthsPt.getOrElse(calendar.get(Calendar.MONTH)) { "" }
        val year = calendar.get(Calendar.YEAR)

        return "$dayOfWeek, dia $dayOfMonth de $month de $year"
    }

    /**
     * Formata o status da bateria para fala clara.
     */
    fun formatBatterySpoken(batteryPercent: Int, isCharging: Boolean): String {
        val base = "A bateria está em $batteryPercent%"
        return if (isCharging) {
            "$base, carregando na tomada"
        } else {
            base
        }
    }

    /**
     * Gera status completo consolidado para dar orientação imediata ao usuário cego.
     */
    fun getCompleteStatus(
        calendar: Calendar = Calendar.getInstance(),
        batteryPercent: Int,
        isCharging: Boolean
    ): String {
        val time = formatTimeSpoken(calendar)
        val date = formatDateSpoken(calendar)
        val battery = formatBatterySpoken(batteryPercent, isCharging)

        return "$time, $date. $battery."
    }
}
