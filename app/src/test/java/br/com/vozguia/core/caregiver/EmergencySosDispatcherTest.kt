package br.com.vozguia.core.caregiver

import br.com.vozguia.core.routine.domain.TrustedContact
import br.com.vozguia.core.routine.fake.FakeTrustedContactRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class EmergencySosDispatcherTest {

    private lateinit var contactRepository: FakeTrustedContactRepository
    private lateinit var dispatcher: EmergencySosDispatcher

    private val gabriel = TrustedContact(
        id = 1L,
        displayName = "Gabriel Silva",
        phoneNumber = "11999998888",
        relationshipRole = "filho",
        phoneticAliases = listOf("gabriel", "biel", "filho"),
        isVipFamily = true,
        isEmergencyContact = true
    )

    @Before
    fun setup() = runTest {
        contactRepository = FakeTrustedContactRepository()
        contactRepository.insertContact(gabriel)
        dispatcher = EmergencySosDispatcher(contactRepository)
    }

    @Test
    fun `dispatches emergency call to configured emergency contact`() = runTest {
        val result = dispatcher.dispatchEmergencySos()

        assertThat(result).isInstanceOf(EmergencySosResult.Executed::class.java)
        val executed = result as EmergencySosResult.Executed
        assertThat(executed.targetContact.displayName).isEqualTo("Gabriel Silva")
        assertThat(executed.spokenAnnouncement).isEqualTo("Ligando agora para emergência: Gabriel Silva.")
        assertThat(executed.phoneNumber).isEqualTo("11999998888")
        assertThat(executed.maxSpeakerVolume).isTrue()
    }

    @Test
    fun `generates emergency WhatsApp message payload for family group or contact`() = runTest {
        val payload = dispatcher.generateEmergencyWhatsAppText(timeHhMm = "15:30")

        assertThat(payload).contains("EMERGÊNCIA SOS")
        assertThat(payload).contains("Papai acionou o pedido de socorro às 15:30")
        assertThat(payload).contains("Por favor, atenda a ligação imediatamente")
    }

    @Test
    fun `handles missing emergency contact gracefully by falling back to 192 SAMU`() = runTest {
        // Clear contacts
        val emptyRepo = FakeTrustedContactRepository()
        val emptyDispatcher = EmergencySosDispatcher(emptyRepo)

        val result = emptyDispatcher.dispatchEmergencySos()
        assertThat(result).isInstanceOf(EmergencySosResult.Executed::class.java)
        val executed = result as EmergencySosResult.Executed
        assertThat(executed.phoneNumber).isEqualTo("192")
        assertThat(executed.targetContact.displayName).isEqualTo("SAMU - Emergência")
    }
}
