package br.com.vozguia.core.routine.utilities

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class DailyAutonomyUtilitiesTest {

    private lateinit var utilities: DailyAutonomyUtilities

    @Before
    fun setup() {
        utilities = DailyAutonomyUtilities()
    }

    @Test
    fun `formats afternoon time into natural spoken Portuguese`() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 20)
        }
        val speech = utilities.formatTimeSpoken(calendar)
        assertThat(speech).isEqualTo("São 3 horas e 20 minutos da tarde")
    }

    @Test
    fun `formats noon and midnight correctly`() {
        val noon = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
        }
        assertThat(utilities.formatTimeSpoken(noon)).isEqualTo("É meio-dia")

        val midnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
        }
        assertThat(utilities.formatTimeSpoken(midnight)).isEqualTo("É meia-noite")
    }

    @Test
    fun `formats one oclock correctly as singular uma hora`() {
        val oneAm = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 5)
        }
        assertThat(utilities.formatTimeSpoken(oneAm)).isEqualTo("É uma hora e 5 minutos da madrugada")

        val onePm = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 13)
            set(Calendar.MINUTE, 0)
        }
        assertThat(utilities.formatTimeSpoken(onePm)).isEqualTo("É uma hora da tarde")
    }

    @Test
    fun `formats date into natural Portuguese with day of week`() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 9) // 2026-11-09 is Monday
        }
        val speech = utilities.formatDateSpoken(calendar)
        assertThat(speech).isEqualTo("segunda-feira, dia 9 de novembro de 2026")
    }

    @Test
    fun `formats battery query for normal unplugged state`() {
        val speech = utilities.formatBatterySpoken(batteryPercent = 78, isCharging = false)
        assertThat(speech).isEqualTo("A bateria está em 78%")
    }

    @Test
    fun `formats battery query for charging state`() {
        val speech = utilities.formatBatterySpoken(batteryPercent = 90, isCharging = true)
        assertThat(speech).isEqualTo("A bateria está em 90%, carregando na tomada")
    }

    @Test
    fun `formats complete combined status query for fast orientation`() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 9)
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 20)
        }
        val status = utilities.getCompleteStatus(calendar, batteryPercent = 78, isCharging = false)
        assertThat(status).contains("São 3 horas e 20 minutos da tarde")
        assertThat(status).contains("segunda-feira, dia 9 de novembro")
        assertThat(status).contains("A bateria está em 78%")
    }
}
