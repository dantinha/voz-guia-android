package br.com.vozguia.core.domain

/**
 * Representa uma intenção de comando identificada a partir da fala do usuário ou gatilho físico.
 * Todas as intenções são imutáveis.
 */
sealed interface UserIntent {

    /**
     * Identifica se a intenção pode ser resolvida exclusivamente de forma local,
     * sem necessidade de modelo de linguagem ou conexão de rede (Custo: 0 tokens / R$ 0,00).
     */
    fun isLocalFastPath(): Boolean

    // ==========================================
    // Comandos Fast-Path (< 50ms)
    // ==========================================

    data object TimeQuery : UserIntent {
        override fun isLocalFastPath(): Boolean = true
    }

    data object DateQuery : UserIntent {
        override fun isLocalFastPath(): Boolean = true
    }

    data object BatteryQuery : UserIntent {
        override fun isLocalFastPath(): Boolean = true
    }

    data object StopOrCancel : UserIntent {
        override fun isLocalFastPath(): Boolean = true
    }

    data object PauseMedia : UserIntent {
        override fun isLocalFastPath(): Boolean = true
    }

    data object ResumeMedia : UserIntent {
        override fun isLocalFastPath(): Boolean = true
    }

    data object NextMedia : UserIntent {
        override fun isLocalFastPath(): Boolean = true
    }

    data object ConfirmYes : UserIntent {
        override fun isLocalFastPath(): Boolean = true
    }

    data object ConfirmNo : UserIntent {
        override fun isLocalFastPath(): Boolean = true
    }

    data object RepeatLast : UserIntent {
        override fun isLocalFastPath(): Boolean = true
    }

    data object EmergencySos : UserIntent {
        override fun isLocalFastPath(): Boolean = true
    }

    // ==========================================
    // Telefonia e Chamadas Unificadas
    // ==========================================

    data class DirectCall(
        val targetName: String,
        val phoneNumber: String? = null,
        val isVoipWhatsApp: Boolean = false
    ) : UserIntent {
        override fun isLocalFastPath(): Boolean = true
    }

    // ==========================================
    // WhatsApp Copiloto
    // ==========================================

    data object ReadUnreadWhatsAppMessages : UserIntent {
        override fun isLocalFastPath(): Boolean = true
    }

    data class PlayWhatsAppAudio(
        val contactName: String? = null
    ) : UserIntent {
        override fun isLocalFastPath(): Boolean = true
    }

    data class SendWhatsAppMessage(
        val targetContact: String,
        val messageText: String
    ) : UserIntent {
        override fun isLocalFastPath(): Boolean = true
    }

    data class OpenWhatsAppChat(
        val targetContact: String
    ) : UserIntent {
        override fun isLocalFastPath(): Boolean = true
    }

    data class SendWhatsAppVoiceNote(
        val targetContact: String
    ) : UserIntent {
        override fun isLocalFastPath(): Boolean = true
    }

    // ==========================================
    // Modo Agente v2.1: Contexto de Tela & Ciclo Vocal
    // ==========================================

    data object DescribeScreenContext : UserIntent {
        override fun isLocalFastPath(): Boolean = true
    }

    data object SleepMode : UserIntent {
        override fun isLocalFastPath(): Boolean = true
    }

    // ==========================================
    // YouTube Modo Rádio
    // ==========================================

    data class YouTubeSearchAndPlay(
        val query: String
    ) : UserIntent {
        override fun isLocalFastPath(): Boolean = true
    }

    data object YouTubeSkipAd : UserIntent {
        override fun isLocalFastPath(): Boolean = true
    }

    // ==========================================
    // Assistência Multimodal em Nuvem (Free Tier)
    // ==========================================

    class DescribeReceivedImage(
        rawImageBytes: ByteArray? = null,
        val contextPrompt: String = "Descreva a imagem recebida de forma direta e concisa para uma pessoa cega."
    ) : UserIntent {
        private val _rawImageBytes: ByteArray? = rawImageBytes?.clone()
        val rawImageBytes: ByteArray? get() = _rawImageBytes?.clone()

        override fun isLocalFastPath(): Boolean = false

        fun copy(
            rawImageBytes: ByteArray? = this._rawImageBytes?.clone(),
            contextPrompt: String = this.contextPrompt
        ): DescribeReceivedImage = DescribeReceivedImage(rawImageBytes, contextPrompt)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as DescribeReceivedImage
            if (_rawImageBytes != null) {
                if (other._rawImageBytes == null) return false
                if (!_rawImageBytes.contentEquals(other._rawImageBytes)) return false
            } else if (other._rawImageBytes != null) return false
            return contextPrompt == other.contextPrompt
        }

        override fun hashCode(): Int {
            var result = _rawImageBytes?.contentHashCode() ?: 0
            result = 31 * result + contextPrompt.hashCode()
            return result
        }
    }

    data class GeneralQuestion(
        val query: String
    ) : UserIntent {
        override fun isLocalFastPath(): Boolean = false
    }

    // ==========================================
    // Intenção Desconhecida / Não Compreendida
    // ==========================================

    data class UnknownIntent(
        val rawTranscription: String
    ) : UserIntent {
        override fun isLocalFastPath(): Boolean = true
    }
}
