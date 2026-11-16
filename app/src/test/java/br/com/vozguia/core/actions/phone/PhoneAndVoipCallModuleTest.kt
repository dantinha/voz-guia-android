package br.com.vozguia.core.actions.phone

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class PhoneAndVoipCallModuleTest {

    private lateinit var callModule: PhoneAndVoipCallModule
    private val familyContacts = listOf(
        FamilyContact(
            id = "1",
            name = "Gabriel",
            phoneNumber = "+5511999990001",
            relationship = "Filho",
            nicknames = listOf("meu filho", "Gabriel", "biel"),
            isTrustedFamily = true
        ),
        FamilyContact(
            id = "2",
            name = "João Silva",
            phoneNumber = "+5511999990002",
            relationship = "Irmão",
            nicknames = listOf("João", "João Silva"),
            isTrustedFamily = true
        ),
        FamilyContact(
            id = "3",
            name = "João Pedro",
            phoneNumber = "+5511999990003",
            relationship = "Primo",
            nicknames = listOf("João", "João Pedro"),
            isTrustedFamily = false
        )
    )

    @Before
    fun setUp() {
        callModule = PhoneAndVoipCallModule()
    }

    @Test
    fun `resolveCallTarget por apelido familiar unico deve iniciar chamada com viva-voz ativo`() {
        val result = callModule.resolveCallTarget("meu filho", familyContacts, isHeadsetConnected = false)

        assertThat(result).isInstanceOf(DialResult.Initiated::class.java)
        val initiated = result as DialResult.Initiated
        assertThat(initiated.contact.name).isEqualTo("Gabriel")
        assertThat(initiated.useSpeakerphone).isTrue()
        assertThat(initiated.spokenAnnouncement).contains("Ligando para Gabriel no viva-voz")
    }

    @Test
    fun `resolveCallTarget com fone conectado nao deve ativar viva-voz`() {
        val result = callModule.resolveCallTarget("Gabriel", familyContacts, isHeadsetConnected = true)

        assertThat(result).isInstanceOf(DialResult.Initiated::class.java)
        val initiated = result as DialResult.Initiated
        assertThat(initiated.useSpeakerphone).isFalse()
        assertThat(initiated.spokenAnnouncement).contains("Ligando para Gabriel")
        assertThat(initiated.spokenAnnouncement).doesNotContain("viva-voz")
    }

    @Test
    fun `resolveCallTarget com nome ambiguo deve perguntar antes de discar`() {
        val result = callModule.resolveCallTarget("João", familyContacts, isHeadsetConnected = false)

        assertThat(result).isInstanceOf(DialResult.Ambiguous::class.java)
        val ambiguous = result as DialResult.Ambiguous
        assertThat(ambiguous.candidates).hasSize(2)
        assertThat(ambiguous.spokenPrompt).contains("Encontrei João Silva e João Pedro. Para qual dos dois você quer ligar?")
    }

    @Test
    fun `resolveCallTarget com contato inexistente deve avisar com voz acolhedora`() {
        val result = callModule.resolveCallTarget("Dr. Marcos", familyContacts, isHeadsetConnected = false)

        assertThat(result).isInstanceOf(DialResult.NotFound::class.java)
        val notFound = result as DialResult.NotFound
        assertThat(notFound.spokenMessage).contains("Não encontrei o contato Dr. Marcos")
    }

    @Test
    fun `formatIncomingCallAnnouncement deve emitir instrucao clara de botao fisico`() {
        val text = callModule.formatIncomingCallAnnouncement("Gabriel")
        assertThat(text).contains("Ligação de Gabriel")
        assertThat(text).contains("Aperte o botão de volume para atender, ou segure para recusar")
    }

    @Test
    fun `handleVolumeKeyDuringRinging deve atender com toque curto e rejeitar com hold longo`() {
        assertThat(callModule.handleVolumeKeyDuringRinging(isShortTap = true))
            .isEqualTo(IncomingCallAction.AcceptCall)

        assertThat(callModule.handleVolumeKeyDuringRinging(isShortTap = false))
            .isEqualTo(IncomingCallAction.RejectCall)
    }
}
