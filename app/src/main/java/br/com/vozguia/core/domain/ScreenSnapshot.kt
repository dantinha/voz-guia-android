package br.com.vozguia.core.domain

/**
 * Representação imutável da tela ativa inspecionada pelo SemanticScreenTreeParser.
 */
data class ScreenSnapshot(
    val packageName: String,
    val windowTitle: String? = null,
    val hasPasswordFields: Boolean = false,
    val isSystemDialog: Boolean = false,
    val actionableElements: List<SemanticElement> = emptyList(),
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * Elemento semântico individual extraído da árvore de acessibilidade.
 */
data class SemanticElement(
    val id: String? = null,
    val text: String? = null,
    val contentDescription: String? = null,
    val isClickable: Boolean = false,
    val isPassword: Boolean = false,
    val bounds: ScreenRect? = null
)

data class ScreenRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val centerX: Int get() = left + (right - left) / 2
    val centerY: Int get() = top + (bottom - top) / 2
}
