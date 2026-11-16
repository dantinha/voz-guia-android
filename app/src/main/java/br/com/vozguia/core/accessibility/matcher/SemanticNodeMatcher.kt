package br.com.vozguia.core.accessibility.matcher

import br.com.vozguia.core.domain.ScreenRect
import br.com.vozguia.core.domain.SemanticElement

/**
 * Critérios em cascata para localização resiliente de elementos na tela (RN-3.02).
 */
data class MatchCriteria(
    val idSuffix: String? = null,
    val textOrDescKeywords: List<String> = emptyList(),
    val positionFilter: ((ScreenRect) -> Boolean)? = null
)

/**
 * Localizador resiliente de nós com estratégia de fallback em 3 passos:
 * 1. Busca por viewIdResourceName
 * 2. Busca por contentDescription ou texto
 * 3. Busca por posição geométrica relativa na tela
 */
class SemanticNodeMatcher {

    fun findFirstMatching(
        elements: List<SemanticElement>,
        criteria: MatchCriteria
    ): SemanticElement? {
        // Passo 1: Busca por ID (se disponível)
        if (!criteria.idSuffix.isNullOrBlank()) {
            val byId = elements.firstOrNull { element ->
                element.isClickable && element.id?.endsWith(":id/${criteria.idSuffix}") == true
            }
            if (byId != null) return byId
        }

        // Passo 2: Busca por texto ou contentDescription
        if (criteria.textOrDescKeywords.isNotEmpty()) {
            val byText = elements.firstOrNull { element ->
                val fullText = "${element.text} ${element.contentDescription}".lowercase()
                element.isClickable && criteria.textOrDescKeywords.any { fullText.contains(it.lowercase()) }
            }
            if (byText != null) return byText
        }

        // Passo 3: Busca por posição relativa na tela
        if (criteria.positionFilter != null) {
            val byPosition = elements.firstOrNull { element ->
                val bounds = element.bounds
                element.isClickable && bounds != null && criteria.positionFilter.invoke(bounds)
            }
            if (byPosition != null) return byPosition
        }

        return null
    }
}
