package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class HalloRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveUserProfile_authenticatedUser_succeeds(): Unit = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repo = HalloRepository(firestore)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveUserProfile(
                displayName = "Alice Test",
                email = ALICE_EMAIL,
                photoUrl = null,
                publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0"
            )
        }

        val profile = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeCurrentUserProfile().first { it != null }
        }
        assertNotNull(profile)
        assertEquals("Alice Test", profile?.displayName)
        assertEquals(ALICE_EMAIL, profile?.email)
    }

    @Test
    fun createMomentAndComment_authenticatedUsers_succeeds(): Unit = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = HalloRepository(firestore)
        val aliceProfile = UserProfile(
            userId = auth.currentUser!!.uid,
            displayName = "Alice",
            email = ALICE_EMAIL,
            publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0"
        )

        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createMoment(
                text = "Halo dunia! Ini momen pertamaku di Halloapp.",
                backgroundStyle = "gradient_blue",
                authorProfile = aliceProfile
            )
        }

        val moments = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeMoments().first { list -> list.isNotEmpty() }
        }
        assertTrue(moments.isNotEmpty())
        val moment = moments.first()
        assertTrue(moment.text.contains("momen pertamaku"))

        // Bob signs in and comments on Alice's moment
        signInTestUser(BOB_EMAIL)
        val bobRepo = HalloRepository(firestore)
        val bobProfile = UserProfile(
            userId = auth.currentUser!!.uid,
            displayName = "Bob",
            email = BOB_EMAIL,
            publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1"
        )

        withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.addComment(
                momentId = moment.momentId,
                commentText = "Keren banget momennya, Alice!",
                authorProfile = bobProfile
            )
        }

        val comments = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeComments(moment.momentId).first { it.isNotEmpty() }
        }
        assertTrue(comments.isNotEmpty())
        assertEquals("Keren banget momennya, Alice!", comments.first().commentText)
    }

    @Test
    fun observeMoments_unauthenticated_failsWithPermissionDenied(): Unit = runBlocking {
        auth.signOut()
        val repo = HalloRepository(firestore)

        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                repo.observeMoments().first()
            }
        }
    }

    companion object {
        const val ALICE_EMAIL = "alice@test.com"
        const val BOB_EMAIL = "bob@test.com"
        const val DEFAULT_TIMEOUT_MS = 8000L
        const val FLOW_TIMEOUT_MS = 6000L
    }
}
