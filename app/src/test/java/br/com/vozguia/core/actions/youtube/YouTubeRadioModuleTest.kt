package br.com.vozguia.core.actions.youtube

import br.com.vozguia.core.domain.ScreenRect
import br.com.vozguia.core.domain.ScreenSnapshot
import br.com.vozguia.core.domain.SemanticElement
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class YouTubeRadioModuleTest {

    private lateinit var youtubeModule: YouTubeRadioModule

    @Before
    fun setUp() {
        youtubeModule = YouTubeRadioModule()
    }

    @Test
    fun `findFirstOrganicVideo deve ignorar anuncios patrocinados e shorts e retornar primeiro video longo`() {
        val screen = ScreenSnapshot(
            packageName = "com.google.android.youtube",
            actionableElements = listOf(
                // 1. Anúncio patrocinado
                SemanticElement(
                    text = "Patrocinado • Curso de Inglês Online",
                    isClickable = true,
                    bounds = ScreenRect(0, 100, 1080, 400)
                ),
                // 2. Carrossel de Shorts
                SemanticElement(
                    text = "Shorts • Melhores momentos 15s",
                    contentDescription = "Vídeo do Shorts",
                    isClickable = true,
                    bounds = ScreenRect(0, 410, 1080, 800)
                ),
                // 3. Primeiro vídeo orgânico longo
                SemanticElement(
                    text = "Música Sertaneja Raiz — As Melhores Seleção Especial",
                    contentDescription = "Música Sertaneja Raiz, duração 1 hora e 20 minutos",
                    isClickable = true,
                    bounds = ScreenRect(0, 810, 1080, 1200)
                )
            )
        )

        val target = youtubeModule.findFirstOrganicVideo(screen)

        assertThat(target).isNotNull()
        assertThat(target?.text).contains("Música Sertaneja Raiz")
    }

    @Test
    fun `findAdSkipButton deve localizar botao de pular anuncio silenciosamente`() {
        val screen = ScreenSnapshot(
            packageName = "com.google.android.youtube",
            actionableElements = listOf(
                SemanticElement(
                    id = "com.google.android.youtube:id/skip_ad_button",
                    text = "Pular anúncio",
                    isClickable = true,
                    bounds = ScreenRect(800, 1400, 1050, 1500)
                )
            )
        )

        val skipButton = youtubeModule.findAdSkipButton(screen)

        assertThat(skipButton).isNotNull()
        assertThat(skipButton?.text).isEqualTo("Pular anúncio")
    }

    @Test
    fun `findConfirmStillWatchingButton deve detectar dialogo de continuar assistindo`() {
        val screen = ScreenSnapshot(
            packageName = "com.google.android.youtube",
            actionableElements = listOf(
                SemanticElement(text = "O vídeo foi pausado. Continuar assistindo?"),
                SemanticElement(
                    text = "Sim",
                    isClickable = true,
                    bounds = ScreenRect(600, 1200, 800, 1300)
                )
            )
        )

        val confirmButton = youtubeModule.findConfirmStillWatchingButton(screen)

        assertThat(confirmButton).isNotNull()
        assertThat(confirmButton?.text).isEqualTo("Sim")
    }
}
