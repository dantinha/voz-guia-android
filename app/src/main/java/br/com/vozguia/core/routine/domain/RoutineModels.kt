package br.com.vozguia.core.routine.domain

/**
 * Entidade de contato confiável com suporte a apelidos afetivos e resolução fonética.
 */
data class TrustedContact(
    val id: Long = 0,
    val displayName: String,
    val phoneNumber: String,
    val whatsappJid: String? = null,
    val relationshipRole: String, // ex: "filho", "esposa", "irmão", "filha"
    val phoneticAliases: List<String> = emptyList(), // ex: ["gabriel", "gabrieu", "biel", "meu filho"]
    val isVipFamily: Boolean = true,
    val isEmergencyContact: Boolean = false
)

/**
 * Período do dia para hábitos de rotina e rádio pessoal.
 */
enum class TimePeriod {
    MANHA,
    TARDE,
    NOITE
}

/**
 * Favorito de mídia (estação de rádio, estilo musical ou canal do YouTube).
 */
data class MediaFavorite(
    val id: Long = 0,
    val title: String,
    val searchQuery: String,
    val timePeriod: TimePeriod,
    val playCount: Int = 0,
    val isCuratedByCaregiver: Boolean = false
)

/**
 * Lembrete falado de remédio ou compromisso da rotina diária.
 */
data class RoutineReminder(
    val id: Long = 0,
    val title: String,
    val timeHhMm: String, // formato "08:00"
    val daysOfWeek: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7), // 1=Domingo, 7=Sábado
    val isActive: Boolean = true,
    val lastConfirmedAt: Long? = null
)

/**
 * Registro de fala não compreendida para aprendizado e diagnóstico do cuidador.
 */
data class UnrecognizedQuery(
    val id: Long = 0,
    val transcribedText: String,
    val timestamp: Long,
    val resolvedAction: String? = null
)

/**
 * Hábito aprendido automaticamente de interação com um contato por faixa de horário (v2.1).
 */
data class ContactInteractionHabit(
    val id: Long = 0,
    val contactName: String,
    val timePeriod: TimePeriod,
    val interactionCount: Int = 1,
    val lastInteractionAt: Long = 0L
)
