package com.jstuart0.personaldiary.data.repository

import com.jstuart0.personaldiary.data.encryption.E2EEncryptionService
import com.jstuart0.personaldiary.data.encryption.UCEEncryptionService
import com.jstuart0.personaldiary.data.local.dao.UserDao
import com.jstuart0.personaldiary.data.local.entity.UserEntity
import com.jstuart0.personaldiary.data.remote.TokenManager
import com.jstuart0.personaldiary.data.remote.api.PersonalDiaryApi
import com.jstuart0.personaldiary.data.remote.model.*
import com.jstuart0.personaldiary.domain.model.EncryptionTier
import com.jstuart0.personaldiary.service.KeyStoreManager
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryTest {

    @MockK
    private lateinit var api: PersonalDiaryApi

    @MockK
    private lateinit var userDao: UserDao

    @MockK
    private lateinit var tokenManager: TokenManager

    @MockK
    private lateinit var keyStoreManager: KeyStoreManager

    @MockK
    private lateinit var e2eEncryptionService: E2EEncryptionService

    @MockK
    private lateinit var uceEncryptionService: UCEEncryptionService

    private lateinit var repository: AuthRepository

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        repository = AuthRepository(
            api = api,
            userDao = userDao,
            tokenManager = tokenManager,
            keyStoreManager = keyStoreManager,
            e2eEncryptionService = e2eEncryptionService,
            uceEncryptionService = uceEncryptionService
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `login with E2E encryption success`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        val tier = EncryptionTier.E2E

        val loginResponse = LoginResponse(
            userId = "user123",
            email = email,
            encryptionTier = tier.name,
            accessToken = "access_token",
            refreshToken = "refresh_token",
            expiresIn = 3600,
            serverPublicKey = "server_public_key",
            encryptedMasterKey = "encrypted_master_key"
        )

        coEvery { api.login(any()) } returns Response.success(loginResponse)
        coEvery { tokenManager.saveTokens(any(), any()) } just Runs
        coEvery { e2eEncryptionService.initialize(any()) } returns Result.success(Unit)
        coEvery { userDao.insert(any()) } just Runs
        coEvery { userDao.getCurrentUserFlow() } returns flowOf(
            UserEntity(
                userId = "user123",
                email = email,
                encryptionTier = tier.name,
                serverPublicKey = "server_public_key",
                encryptedMasterKey = "encrypted_master_key",
                createdAt = System.currentTimeMillis()
            )
        )

        // When
        val result = repository.login(email, password, tier)

        // Then
        assertTrue(result.isSuccess)
        val user = result.getOrNull()
        assertNotNull(user)
        assertEquals(email, user?.email)
        assertEquals(tier, user?.encryptionTier)

        coVerify {
            api.login(match { it.email == email && it.password == password })
            tokenManager.saveTokens("access_token", "refresh_token")
            e2eEncryptionService.initialize(password)
            userDao.insert(any())
        }
    }

    @Test
    fun `login with UCE encryption success`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        val tier = EncryptionTier.UCE

        val loginResponse = LoginResponse(
            userId = "user123",
            email = email,
            encryptionTier = tier.name,
            accessToken = "access_token",
            refreshToken = "refresh_token",
            expiresIn = 3600,
            serverPublicKey = null,
            encryptedMasterKey = null
        )

        coEvery { api.login(any()) } returns Response.success(loginResponse)
        coEvery { tokenManager.saveTokens(any(), any()) } just Runs
        coEvery { uceEncryptionService.initialize(any()) } returns Result.success(Unit)
        coEvery { userDao.insert(any()) } just Runs
        coEvery { userDao.getCurrentUserFlow() } returns flowOf(
            UserEntity(
                userId = "user123",
                email = email,
                encryptionTier = tier.name,
                serverPublicKey = null,
                encryptedMasterKey = null,
                createdAt = System.currentTimeMillis()
            )
        )

        // When
        val result = repository.login(email, password, tier)

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            uceEncryptionService.initialize(password)
        }
    }

    @Test
    fun `login with invalid credentials returns failure`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "wrong_password"

        coEvery { api.login(any()) } returns Response.error(
            401,
            "Unauthorized".toResponseBody()
        )

        // When
        val result = repository.login(email, password, EncryptionTier.E2E)

        // Then
        assertTrue(result.isFailure)
        coVerify(exactly = 0) {
            tokenManager.saveTokens(any(), any())
            userDao.insert(any())
        }
    }

    @Test
    fun `signup success creates user and initializes encryption`() = runTest {
        // Given
        val email = "newuser@example.com"
        val password = "password123"
        val tier = EncryptionTier.E2E

        val signupResponse = SignupResponse(
            userId = "user123",
            email = email,
            encryptionTier = tier.name,
            accessToken = "access_token",
            refreshToken = "refresh_token",
            expiresIn = 3600,
            serverPublicKey = "server_public_key",
            recoveryCodes = listOf("code1", "code2", "code3")
        )

        coEvery { e2eEncryptionService.generateKeyPair() } returns Result.success("public_key")
        coEvery { api.signup(any()) } returns Response.success(signupResponse)
        coEvery { tokenManager.saveTokens(any(), any()) } just Runs
        coEvery { e2eEncryptionService.initialize(any()) } returns Result.success(Unit)
        coEvery { userDao.insert(any()) } just Runs
        coEvery { userDao.getCurrentUserFlow() } returns flowOf(
            UserEntity(
                userId = "user123",
                email = email,
                encryptionTier = tier.name,
                serverPublicKey = "server_public_key",
                encryptedMasterKey = null,
                createdAt = System.currentTimeMillis()
            )
        )

        // When
        val result = repository.signup(email, password, tier)

        // Then
        assertTrue(result.isSuccess)
        val signupResult = result.getOrNull()
        assertNotNull(signupResult)
        assertEquals(email, signupResult?.user?.email)
        assertEquals(3, signupResult?.recoveryCodes?.size)

        coVerify {
            e2eEncryptionService.generateKeyPair()
            api.signup(any())
            e2eEncryptionService.initialize(password)
        }
    }

    @Test
    fun `logout clears tokens and user data`() = runTest {
        // Given
        val userEntity = UserEntity(
            userId = "user123",
            email = "test@example.com",
            encryptionTier = EncryptionTier.E2E.name,
            serverPublicKey = "public_key",
            encryptedMasterKey = "encrypted_key",
            createdAt = System.currentTimeMillis()
        )

        coEvery { userDao.getCurrentUser() } returns userEntity
        coEvery { api.logout() } returns Response.success(Unit)
        coEvery { tokenManager.clearTokens() } just Runs
        coEvery { userDao.delete(any()) } just Runs
        coEvery { keyStoreManager.clearAllKeys() } just Runs

        // When
        repository.logout()

        // Then
        coVerify {
            api.logout()
            tokenManager.clearTokens()
            userDao.delete(userEntity)
            keyStoreManager.clearAllKeys()
        }
    }

    @Test
    fun `getCurrentUser returns current user from dao`() = runTest {
        // Given
        val userEntity = UserEntity(
            userId = "user123",
            email = "test@example.com",
            encryptionTier = EncryptionTier.E2E.name,
            serverPublicKey = "public_key",
            encryptedMasterKey = "encrypted_key",
            createdAt = System.currentTimeMillis()
        )

        coEvery { userDao.getCurrentUserFlow() } returns flowOf(userEntity)

        // When
        val user = repository.currentUser.first()

        // Then
        assertNotNull(user)
        assertEquals("user123", user?.userId)
        assertEquals("test@example.com", user?.email)
    }

    @Test
    fun `isAuthenticated returns true when user exists`() = runTest {
        // Given
        val userEntity = UserEntity(
            userId = "user123",
            email = "test@example.com",
            encryptionTier = EncryptionTier.E2E.name,
            serverPublicKey = "public_key",
            encryptedMasterKey = "encrypted_key",
            createdAt = System.currentTimeMillis()
        )

        coEvery { userDao.getCurrentUser() } returns userEntity

        // When
        val isAuthenticated = repository.isAuthenticated.first()

        // Then
        assertTrue(isAuthenticated)
    }

    @Test
    fun `isAuthenticated returns false when no user exists`() = runTest {
        // Given
        coEvery { userDao.getCurrentUser() } returns null

        // When
        val isAuthenticated = repository.isAuthenticated.first()

        // Then
        assertFalse(isAuthenticated)
    }
}
