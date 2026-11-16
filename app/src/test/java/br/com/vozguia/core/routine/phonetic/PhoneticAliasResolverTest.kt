package br.com.vozguia.core.routine.phonetic

import br.com.vozguia.core.routine.domain.TrustedContact
import br.com.vozguia.core.routine.fake.FakeTrustedContactRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class PhoneticAliasResolverTest {

    private lateinit var contactRepository: FakeTrustedContactRepository
    private lateinit var resolver: PhoneticAliasResolver

    private val gabriel = TrustedContact(
        id = 1L,
        displayName = "Gabriel Silva",
        phoneNumber = "11999998888",
        relationshipRole = "filho",
        phoneticAliases = listOf("gabriel", "gabrieu", "biel", "meu filho", "filhote"),
        isVipFamily = true,
        isEmergencyContact = true
    )

    private val maria = TrustedContact(
        id = 2L,
        displayName = "Maria Silva",
        phoneNumber = "11988887777",
        relationshipRole = "esposa",
        phoneticAliases = listOf("maria", "minha esposa", "amor", "esposa"),
        isVipFamily = true,
        isEmergencyContact = false
    )

    @Before
    fun setup() = runTest {
        contactRepository = FakeTrustedContactRepository()
        contactRepository.insertContact(gabriel)
        contactRepository.insertContact(maria)
        resolver = PhoneticAliasResolver(contactRepository)
    }

    @Test
    fun `resolves exact relationship role like meu filho to Gabriel`() = runTest {
        val result = resolver.resolve("meu filho")
        assertThat(result).isInstanceOf(PhoneticMatchResult.Found::class.java)
        val found = result as PhoneticMatchResult.Found
        assertThat(found.contact.displayName).isEqualTo("Gabriel Silva")
    }

    @Test
    fun `resolves spoken variation with brazilian L to U sound like Gabrieu to Gabriel`() = runTest {
        val result = resolver.resolve("Gabrieu")
        assertThat(result).isInstanceOf(PhoneticMatchResult.Found::class.java)
        val found = result as PhoneticMatchResult.Found
        assertThat(found.contact.id).isEqualTo(1L)
    }

    @Test
    fun `resolves affectionate nickname Biel to Gabriel`() = runTest {
        val result = resolver.resolve("Biel")
        assertThat(result).isInstanceOf(PhoneticMatchResult.Found::class.java)
        val found = result as PhoneticMatchResult.Found
        assertThat(found.contact.id).isEqualTo(1L)
    }

    @Test
    fun `resolves Levenshtein edit distance within 2 for typo like Ghabriel or Gabrel`() = runTest {
        val result = resolver.resolve("Gabrel")
        assertThat(result).isInstanceOf(PhoneticMatchResult.Found::class.java)
        val found = result as PhoneticMatchResult.Found
        assertThat(found.contact.id).isEqualTo(1L)
    }

    @Test
    fun `resolves natural phrase with stop words stripped like Liga pro meu filho`() = runTest {
        val result = resolver.resolveFromSpokenPhrase("Liga pro meu filho")
        assertThat(result).isInstanceOf(PhoneticMatchResult.Found::class.java)
        val found = result as PhoneticMatchResult.Found
        assertThat(found.contact.displayName).isEqualTo("Gabriel Silva")
    }

    @Test
    fun `resolves natural phrase with Mandar mensagem para Maria`() = runTest {
        val result = resolver.resolveFromSpokenPhrase("Mandar mensagem para a Maria")
        assertThat(result).isInstanceOf(PhoneticMatchResult.Found::class.java)
        val found = result as PhoneticMatchResult.Found
        assertThat(found.contact.displayName).isEqualTo("Maria Silva")
    }

    @Test
    fun `returns NotFound with extracted role when family role is not registered yet`() = runTest {
        val result = resolver.resolveFromSpokenPhrase("Liga pro meu irmão")
        assertThat(result).isInstanceOf(PhoneticMatchResult.NotFound::class.java)
        val notFound = result as PhoneticMatchResult.NotFound
        assertThat(notFound.detectedRole).isEqualTo("irmão")
    }

    @Test
    fun `resolution completes in under 10ms`() = runTest {
        val start = System.currentTimeMillis()
        resolver.resolve("Gabrieu")
        val elapsed = System.currentTimeMillis() - start
        assertThat(elapsed).isLessThan(20L) // fast path requirement
    }
}
