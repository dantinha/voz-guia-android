package br.com.vozguia.core.routine.phonetic

import br.com.vozguia.core.routine.domain.TrustedContact
import br.com.vozguia.core.routine.fake.FakeTrustedContactRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ConversationalAliasLearnerTest {

    private lateinit var contactRepository: FakeTrustedContactRepository
    private lateinit var learner: ConversationalAliasLearner

    private val carlos = TrustedContact(
        id = 10L,
        displayName = "Carlos Almeida",
        phoneNumber = "11977776666",
        relationshipRole = "amigo",
        phoneticAliases = listOf("carlos"),
        isVipFamily = false
    )

    @Before
    fun setup() = runTest {
        contactRepository = FakeTrustedContactRepository()
        contactRepository.insertContact(carlos)
        learner = ConversationalAliasLearner(contactRepository)
    }

    @Test
    fun `starts learning session when family role is unknown and asks who it is`() = runTest {
        val state = learner.startLearning(unmappedRole = "irmão")

        assertThat(state).isInstanceOf(AliasLearningState.AwaitingCandidateName::class.java)
        val prompt = (state as AliasLearningState.AwaitingCandidateName).spokenPrompt
        assertThat(prompt).contains("Qual é o nome do seu irmão")
    }

    @Test
    fun `matches candidate name to agenda and asks confirmation`() = runTest {
        learner.startLearning(unmappedRole = "irmão")
        val state = learner.processCandidateName(candidateName = "Carlos")

        assertThat(state).isInstanceOf(AliasLearningState.AwaitingConfirmation::class.java)
        val confirmationState = state as AliasLearningState.AwaitingConfirmation
        assertThat(confirmationState.candidateContact.displayName).isEqualTo("Carlos Almeida")
        assertThat(confirmationState.spokenPrompt).contains("Carlos Almeida")
        assertThat(confirmationState.spokenPrompt).contains("irmão")
    }

    @Test
    fun `confirms association and permanently saves alias and role in SQLite`() = runTest {
        learner.startLearning(unmappedRole = "irmão")
        learner.processCandidateName(candidateName = "Carlos")
        val finalState = learner.confirmAssociation(confirmed = true)

        assertThat(finalState).isInstanceOf(AliasLearningState.LearnedSuccessfully::class.java)
        val success = finalState as AliasLearningState.LearnedSuccessfully
        assertThat(success.spokenConfirmation).contains("Carlos Almeida é seu irmão")

        // Verify updated contact in repository
        val updatedContact = contactRepository.getContactById(10L)
        assertThat(updatedContact).isNotNull()
        assertThat(updatedContact!!.phoneticAliases).contains("irmão")
        assertThat(updatedContact.phoneticAliases).contains("meu irmão")
    }

    @Test
    fun `cancels learning when user rejects association`() = runTest {
        learner.startLearning(unmappedRole = "irmão")
        learner.processCandidateName(candidateName = "Carlos")
        val finalState = learner.confirmAssociation(confirmed = false)

        assertThat(finalState).isInstanceOf(AliasLearningState.Cancelled::class.java)

        // Verify contact was not changed
        val unchangedContact = contactRepository.getContactById(10L)
        assertThat(unchangedContact!!.phoneticAliases).doesNotContain("irmão")
    }
}
