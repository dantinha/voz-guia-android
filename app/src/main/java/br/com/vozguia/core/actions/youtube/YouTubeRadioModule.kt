package br.com.vozguia.core.actions.youtube

import br.com.vozguia.core.domain.ScreenSnapshot
import br.com.vozguia.core.domain.SemanticElement

/**
 * Módulo YouTube Modo Rádio Blindado (FEAT-3.3).
 * Busca direta sem anúncios patrocinados, bloqueio de Shorts,
 * pulador silencioso de propagandas (Ad-Skipper) e confirmação de reprodução contínua.
 */
class YouTubeRadioModule {

    private val adSkipKeywords = listOf(
        "pular anúncio", "pular anúncios", "skip ad", "skip ads", "pular"
    )

    private val sponsoredKeywords = listOf(
        "patrocinado", "anúncio", "ad •", "propaganda"
    )

    fun findFirstOrganicVideo(screen: ScreenSnapshot): SemanticElement? {
        if (screen.packageName != "com.google.android.youtube") return null

        return screen.actionableElements.firstOrNull { element ->
            val text = element.text ?: ""
            val desc = element.contentDescription ?: ""
            val fullContent = "$text $desc".lowercase()

            val isClickable = element.isClickable
            val isSponsored = sponsoredKeywords.any { fullContent.contains(it) }
            val isShorts = fullContent.contains("shorts")
            val hasValidTitle = text.isNotBlank() || desc.isNotBlank()

            isClickable && hasValidTitle && !isSponsored && !isShorts
        }
    }

    fun findAdSkipButton(screen: ScreenSnapshot): SemanticElement? {
        if (screen.packageName != "com.google.android.youtube") return null

        return screen.actionableElements.firstOrNull { element ->
            val id = element.id ?: ""
            val text = element.text?.lowercase() ?: ""
            val desc = element.contentDescription?.lowercase() ?: ""

            val matchesId = id.contains("skip_ad", ignoreCase = true)
            val matchesText = adSkipKeywords.any { text.contains(it) || desc.contains(it) }

            element.isClickable && (matchesId || matchesText)
        }
    }

    fun findConfirmStillWatchingButton(screen: ScreenSnapshot): SemanticElement? {
        if (screen.packageName != "com.google.android.youtube") return null

        val hasStillWatchingDialog = screen.actionableElements.any { element ->
            val full = "${element.text} ${element.contentDescription}".lowercase()
            full.contains("continuar assistindo") || full.contains("vídeo pausado")
        }

        if (!hasStillWatchingDialog) return null

        return screen.actionableElements.firstOrNull { element ->
            val text = element.text?.lowercase() ?: ""
            element.isClickable && (text == "sim" || text == "continuar" || text == "yes")
        }
    }
}
