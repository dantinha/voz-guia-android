package br.com.vozguia.core.routine.media

import br.com.vozguia.core.routine.data.MediaFavoriteRepository
import br.com.vozguia.core.routine.domain.TimePeriod
import java.util.Calendar

data class SpokenMediaRecommendation(
    val title: String,
    val searchQuery: String,
    val spokenAnnouncement: String
)

class RoutineHabitLearner(
    private val favoriteRepository: MediaFavoriteRepository
) {

    /**
     * Resolve um comando genérico de mídia baseado no período do dia e hábitos salvos.
     */
    suspend fun resolveGenericMediaCommand(
        userPrompt: String,
        calendar: Calendar = Calendar.getInstance()
    ): SpokenMediaRecommendation {
        val period = determineTimePeriod(calendar)
        val favorites = favoriteRepository.getFavoritesForPeriod(period)

        val selected = favorites.firstOrNull()
        return if (selected != null) {
            SpokenMediaRecommendation(
                title = selected.title,
                searchQuery = selected.searchQuery,
                spokenAnnouncement = "Colocando seu favorito: ${selected.title}."
            )
        } else {
            // Fallback padrão acolhedor por período
            val fallback = when (period) {
                TimePeriod.MANHA -> SpokenMediaRecommendation(
                    title = "Notícias do Dia",
                    searchQuery = "jornal noticias do dia ao vivo",
                    spokenAnnouncement = "Colocando as notícias do dia."
                )
                TimePeriod.TARDE -> SpokenMediaRecommendation(
                    title = "Músicas Sertanejas Clássicas",
                    searchQuery = "moda de viola sertanejo raiz anos 80",
                    spokenAnnouncement = "Colocando clássicos da música sertaneja."
                )
                TimePeriod.NOITE -> SpokenMediaRecommendation(
                    title = "Músicas Clássicas e Calmas",
                    searchQuery = "musicas instrumentais calmas para relaxar",
                    spokenAnnouncement = "Colocando músicas calmas para o fim do dia."
                )
            }
            fallback
        }
    }

    /**
     * Registra o incremento de reprodução do favorito para refinamento contínuo.
     */
    suspend fun recordPlayback(favoriteId: Long) {
        favoriteRepository.incrementPlayCount(favoriteId)
    }

    private fun determineTimePeriod(calendar: Calendar): TimePeriod {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> TimePeriod.MANHA
            in 12..17 -> TimePeriod.TARDE
            else -> TimePeriod.NOITE
        }
    }
}
