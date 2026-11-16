package br.com.vozguia.core.caregiver

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class BatteryAndWatchdogMonitorTest {

    private lateinit var monitor: BatteryAndWatchdogMonitor

    @Before
    fun setup() {
        monitor = BatteryAndWatchdogMonitor()
    }

    @Test
    fun `announces immediately when power charger is connected`() {
        val announcement = monitor.onPowerConnected(batteryPercent = 45)
        assertThat(announcement).isNotNull()
        assertThat(announcement).contains("Carregador conectado")
        assertThat(announcement).contains("Bateria em 45 por cento")
    }

    @Test
    fun `announces when power charger is disconnected`() {
        val announcement = monitor.onPowerDisconnected()
        assertThat(announcement).isEqualTo("Carregador desconectado.")
    }

    @Test
    fun `announces low battery warning at 20 percent when unplugged`() {
        val announcement = monitor.onBatteryLevelChanged(percent = 20, isCharging = false)
        assertThat(announcement).isNotNull()
        assertThat(announcement).contains("Atenção: a bateria do celular está fraca, com 20 por cento")
        assertThat(announcement).contains("Por favor, coloque para carregar")
    }

    @Test
    fun `announces critical battery warning at 10 percent when unplugged`() {
        val announcement = monitor.onBatteryLevelChanged(percent = 10, isCharging = false)
        assertThat(announcement).isNotNull()
        assertThat(announcement).contains("Atenção urgente: a bateria do celular está em 10 por cento")
    }

    @Test
    fun `does not announce low battery when phone is charging`() {
        val announcement = monitor.onBatteryLevelChanged(percent = 15, isCharging = true)
        assertThat(announcement).isNull()
    }

    @Test
    fun `does not spam warning if battery remains at same low level`() {
        val firstAlert = monitor.onBatteryLevelChanged(percent = 20, isCharging = false)
        assertThat(firstAlert).isNotNull()

        // Second event with same 20% level
        val duplicateAlert = monitor.onBatteryLevelChanged(percent = 20, isCharging = false)
        assertThat(duplicateAlert).isNull()
    }
}
