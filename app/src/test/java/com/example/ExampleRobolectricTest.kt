package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.VaultRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Secure Gallery", appName)
    }

    @Test
    fun `test repository initialization and authentication`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = VaultRepository(context)
        repository.initializeIfNeeded()

        // Test valid authentication for pre-seeded user
        val user = repository.authenticateVisitor("alice", "alicepassword")
        assertNotNull("Alice should be authenticated", user)
        assertEquals("Alice Walker", user?.displayName)

        // Test invalid password
        val invalidUser = repository.authenticateVisitor("alice", "wrongpassword")
        assertNull("Invalid password should return null", invalidUser)

        // Test admin passcode verification
        val adminValid = repository.verifyAdminPasscode("admin123")
        assertTrue("Default admin passcode should be valid", adminValid)
    }
}
