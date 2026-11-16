package br.com.vozguia.core.routine.media

import br.com.vozguia.core.routine.domain.MediaFavorite
import br.com.vozguia.core.routine.domain.TimePeriod
import br.com.vozguia.core.routine.fake.FakeMediaFavoriteRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class RoutineHabitLearnerTest {

    private lateinit var repository: FakeMediaFavoriteRepository
    private lateinit var learner: RoutineHabitLearner

    private val morningNews = MediaFavorite(
        id = 1L,
        title = "Jornal da Manhã",
        searchQuery = "jornal da manha ao vivo",
        timePeriod = TimePeriod.MANHA,
        playCount = 15,
        isCuratedByCaregiver = true
    )

    private val afternoonViola = MediaFavorite(
        id = 2L,
        title = "Moda de Viola Raiz",
        searchQuery = "moda de viola anos 80 completa",
        timePeriod = TimePeriod.TARDE,
        playCount = 30,
        isCuratedByCaregiver = false
    )

    @Before
    fun setup() = runTest {
        repository = FakeMediaFavoriteRepository()
        repository.insertFavorite(morningNews)
        repository.insertFavorite(afternoonViola)
        learner = RoutineHabitLearner(repository)
    }

    @Test
    fun `resolves generic morning music command to morning favorite`() = runTest {
        val morningCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
        }
        val media = learner.resolveGenericMediaCommand("Toca minhas notícias", morningCal)

        assertThat(media.title).isEqualTo("Jornal da Manhã")
        assertThat(media.searchQuery).isEqualTo("jornal da manha ao vivo")
        assertThat(media.spokenAnnouncement).contains("Colocando seu favorito: Jornal da Manhã")
    }

    @Test
    fun `resolves generic afternoon music command to afternoon favorite`() = runTest {
        val afternoonCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 14)
        }
        val media = learner.resolveGenericMediaCommand("Coloca uma música", afternoonCal)

        assertThat(media.title).isEqualTo("Moda de Viola Raiz")
        assertThat(media.searchQuery).isEqualTo("moda de viola anos 80 completa")
        assertThat(media.spokenAnnouncement).contains("Colocando seu favorito: Moda de Viola Raiz")
    }

    @Test
    fun `increments play count when favorite is played`() = runTest {
        learner.recordPlayback(favoriteId = 2L)
        val all = repository.getAllFavorites()
        val updated = all.find { it.id == 2L }
        assertThat(updated?.playCount).isEqualTo(31)
    }

    @Test
    fun `falls back to safe default if no favorite is configured for time period`() = runTest {
        val nightCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 21)
        }
        val media = learner.resolveGenericMediaCommand("Toca uma música", nightCal)

        assertThat(media.title).isNotEmpty()
        assertThat(media.searchQuery).isNotEmpty()
        assertThat(media.spokenAnnouncement).contains("Colocando")
    }
}
