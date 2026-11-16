package br.com.vozguia.core.caregiver

class BatteryAndWatchdogMonitor {

    private var lastAnnouncedLowBatteryLevel: Int? = null

    /**
     * Chamado quando o cabo de carregamento é plugado no aparelho.
     */
    fun onPowerConnected(batteryPercent: Int): String {
        lastAnnouncedLowBatteryLevel = null // reset alertas
        return "Carregador conectado. Bateria em $batteryPercent por cento."
    }

    /**
     * Chamado quando o carregador é retirado.
     */
    fun onPowerDisconnected(): String {
        return "Carregador desconectado."
    }

    /**
     * Avalia mudanças no nível de bateria para emitir alertas falados preventivos ao usuário cego.
     */
    fun onBatteryLevelChanged(percent: Int, isCharging: Boolean): String? {
        if (isCharging) {
            lastAnnouncedLowBatteryLevel = null
            return null
        }

        return when {
            percent <= 10 && lastAnnouncedLowBatteryLevel != 10 -> {
                lastAnnouncedLowBatteryLevel = 10
                "Atenção urgente: a bateria do celular está em 10 por cento. O celular vai desligar em breve se não for colocado na tomada."
            }
            percent <= 20 && percent > 10 && lastAnnouncedLowBatteryLevel != 20 -> {
                lastAnnouncedLowBatteryLevel = 20
                "Atenção: a bateria do celular está fraca, com 20 por cento. Por favor, coloque para carregar."
            }
            percent > 20 -> {
                lastAnnouncedLowBatteryLevel = null
                null
            }
            else -> null
        }
    }
}
