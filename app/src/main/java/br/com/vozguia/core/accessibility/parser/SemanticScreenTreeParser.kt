package br.com.vozguia.core.accessibility.parser

import br.com.vozguia.core.domain.ScreenSnapshot
import br.com.vozguia.core.domain.SemanticElement

/**
 * Extrator semântico de UI da árvore de acessibilidade.
 * Limpa o ruído de containers vazios e elementos invisíveis, retornando
 * apenas elementos úteis e informando presença de campos de senha.
 *
 * Respeita os limites RN-1.02: maxDepth = 18 e maxNodes = 250 para tempo de parse < 35ms.
 */
class SemanticScreenTreeParser(
    private val maxDepth: Int = 18,
    private val maxNodes: Int = 250
) {

    fun parse(
        root: AccessibleNode?,
        packageName: String,
        windowTitle: String? = null
    ): ScreenSnapshot {
        if (root == null) {
            return ScreenSnapshot(packageName = packageName, windowTitle = windowTitle)
        }

        val actionableElements = mutableListOf<SemanticElement>()
        var hasPasswordFields = false
        var nodeCount = 0

        fun traverse(node: AccessibleNode, depth: Int) {
            if (depth >= maxDepth || nodeCount >= maxNodes) return
            if (!node.isVisibleToUser) return

            nodeCount++

            if (node.isPassword) {
                hasPasswordFields = true
            }

            val text = node.text?.toString()?.trim()
            val description = node.contentDescription?.toString()?.trim()
            val isClickable = node.isClickable
            val isPassword = node.isPassword

            val hasUsefulContent = !text.isNullOrBlank() || !description.isNullOrBlank()

            // Filtro de ruído: descarta containers estruturais vazios sem texto nem descrição
            if (hasUsefulContent || isClickable || isPassword) {
                actionableElements.add(
                    SemanticElement(
                        id = node.viewIdResourceName,
                        text = text,
                        contentDescription = description,
                        isClickable = isClickable,
                        isPassword = isPassword,
                        bounds = node.bounds
                    )
                )
            }

            for (i in 0 until node.childCount) {
                if (nodeCount >= maxNodes) break
                val child = node.getChild(i) ?: continue
                traverse(child, depth + 1)
            }
        }

        traverse(root, depth = 0)

        val isSystemDialog = packageName.contains("systemui", ignoreCase = true) ||
                packageName.contains("com.android.server", ignoreCase = true) ||
                packageName == "android"

        return ScreenSnapshot(
            packageName = packageName,
            windowTitle = windowTitle,
            hasPasswordFields = hasPasswordFields,
            isSystemDialog = isSystemDialog,
            actionableElements = actionableElements
        )
    }
}
