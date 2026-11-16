package br.com.vozguia.core.voice.router

import br.com.vozguia.core.domain.UserIntent
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class HybridIntentRouterTest {

    private lateinit var router: HybridIntentRouter

    @Before
    fun setUp() {
        router = HybridIntentRouter()
    }

    // ==========================================
    // Testes de Controle Imediato Fast-Path
    // ==========================================

    @Test
    fun `deve rotear comandos de socorro e emergencia para EmergencySos`() {
        val variants = listOf("socorro", "emergência", "preciso de ajuda", "me ajuda", "chamar socorro", "estou passando mal")
        for (phrase in variants) {
            val intent = router.route(phrase)
            assertThat(intent).isEqualTo(UserIntent.EmergencySos)
        }
    }

    @Test
    fun `deve rotear comandos de parar e cancelar para StopOrCancel`() {
        val variants = listOf("para", "parar", "cancela", "cancelar", "cala a boca", "para de falar", "silêncio")
        for (phrase in variants) {
            val intent = router.route(phrase)
            assertThat(intent).isEqualTo(UserIntent.StopOrCancel)
        }
    }

    @Test
    fun `deve rotear afirmacoes para ConfirmYes`() {
        val variants = listOf("sim", "pode mandar", "manda", "pode enviar", "isso", "confirmo", "enviar")
        for (phrase in variants) {
            val intent = router.route(phrase)
            assertThat(intent).isEqualTo(UserIntent.ConfirmYes)
        }
    }

    @Test
    fun `deve rotear negacoes para ConfirmNo`() {
        val variants = listOf("não", "nao", "deixa quieto", "espera", "não manda")
        for (phrase in variants) {
            val intent = router.route(phrase)
            assertThat(intent).isEqualTo(UserIntent.ConfirmNo)
        }
    }

    @Test
    fun `deve rotear pedidos de repeticao para RepeatLast`() {
        val variants = listOf("repete", "não entendi", "o que você disse", "como é", "repita por favor")
        for (phrase in variants) {
            val intent = router.route(phrase)
            assertThat(intent).isEqualTo(UserIntent.RepeatLast)
        }
    }

    @Test
    fun `deve rotear comandos de midia e YouTube`() {
        assertThat(router.route("pausa")).isEqualTo(UserIntent.PauseMedia)
        assertThat(router.route("pausar")).isEqualTo(UserIntent.PauseMedia)
        assertThat(router.route("continua")).isEqualTo(UserIntent.ResumeMedia)
        assertThat(router.route("continua tocando")).isEqualTo(UserIntent.ResumeMedia)
        assertThat(router.route("próximo")).isEqualTo(UserIntent.NextMedia)
        assertThat(router.route("pular anúncio")).isEqualTo(UserIntent.YouTubeSkipAd)
        assertThat(router.route("pula anúncio")).isEqualTo(UserIntent.YouTubeSkipAd)
    }

    // ==========================================
    // Testes de Utilidades Locais Fast-Path
    // ==========================================

    @Test
    fun `deve rotear consultas de horas, data e bateria`() {
        assertThat(router.route("que horas são")).isEqualTo(UserIntent.TimeQuery)
        assertThat(router.route("horas")).isEqualTo(UserIntent.TimeQuery)
        assertThat(router.route("que dia é hoje")).isEqualTo(UserIntent.DateQuery)
        assertThat(router.route("qual é a data")).isEqualTo(UserIntent.DateQuery)
        assertThat(router.route("quanta bateria tem")).isEqualTo(UserIntent.BatteryQuery)
        assertThat(router.route("nível de bateria")).isEqualTo(UserIntent.BatteryQuery)
    }

    // ==========================================
    // Testes de WhatsApp e Telefone
    // ==========================================

    @Test
    fun `deve rotear checagem de mensagens e play de audios`() {
        assertThat(router.route("tem mensagem nova")).isEqualTo(UserIntent.ReadUnreadWhatsAppMessages)
        assertThat(router.route("o que mandaram no zap")).isEqualTo(UserIntent.ReadUnreadWhatsAppMessages)
        assertThat(router.route("toca o áudio")).isEqualTo(UserIntent.PlayWhatsAppAudio())
        assertThat(router.route("toca o áudio do Gabriel")).isEqualTo(UserIntent.PlayWhatsAppAudio(contactName = "Gabriel"))
    }

    @Test
    fun `deve extrair contato e mensagem no envio guiado de WhatsApp`() {
        val intent = router.route("manda mensagem pro Gabriel dizendo que já tomei o remédio")
        assertThat(intent).isInstanceOf(UserIntent.SendWhatsAppMessage::class.java)

        val waIntent = intent as UserIntent.SendWhatsAppMessage
        assertThat(waIntent.targetContact).isEqualTo("Gabriel")
        assertThat(waIntent.messageText).isEqualTo("já tomei o remédio")
    }

    @Test
    fun `deve extrair nome do contato para chamada telefonica`() {
        val intent = router.route("liga pro Gabriel")
        assertThat(intent).isInstanceOf(UserIntent.DirectCall::class.java)

        val callIntent = intent as UserIntent.DirectCall
        assertThat(callIntent.targetName).isEqualTo("Gabriel")
    }

    // ==========================================
    // Testes de YouTube Busca e Nuvem
    // ==========================================

    @Test
    fun `deve extrair busca musical para YouTubeSearchAndPlay`() {
        val intent = router.route("toca música sertaneja no YouTube")
        assertThat(intent).isInstanceOf(UserIntent.YouTubeSearchAndPlay::class.java)

        val ytIntent = intent as UserIntent.YouTubeSearchAndPlay
        assertThat(ytIntent.query).contains("música sertaneja")
    }

    @Test
    fun `deve rotear pedido de descricao de foto para DescribeReceivedImage`() {
        val intent = router.route("o que tem nessa foto que me mandaram?")
        assertThat(intent).isInstanceOf(UserIntent.DescribeReceivedImage::class.java)
    }

    @Test
    fun `frase nao reconhecida cai em GeneralQuestion ou UnknownIntent`() {
        val intent = router.route("abracadabra batatinha")
        assertThat(intent).isInstanceOf(UserIntent.UnknownIntent::class.java)
    }
}
