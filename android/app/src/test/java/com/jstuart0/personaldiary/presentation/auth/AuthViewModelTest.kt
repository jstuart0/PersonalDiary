package com.jstuart0.personaldiary.presentation.auth

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.jstuart0.personaldiary.data.repository.AuthRepository
import com.jstuart0.personaldiary.domain.model.EncryptionTier
import com.jstuart0.personaldiary.domain.model.RecoveryCode
import com.jstuart0.personaldiary.domain.model.SignupResult
import com.jstuart0.personaldiary.domain.model.User
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @MockK
    private lateinit var authRepository: AuthRepository

    private lateinit var viewModel: AuthViewModel

    private val testUser = User(
        userId = "user123",
        email = "test@example.com",
        encryptionTier = EncryptionTier.E2E,
        serverPublicKey = "public_key",
        encryptedMasterKey = "encrypted_key",
        createdAt = System.currentTimeMillis()
    )

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        coEvery { authRepository.currentUser } returns flowOf(null)
        coEvery { authRepository.isAuthenticated } returns flowOf(false)

        viewModel = AuthViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `login with valid credentials succeeds`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        val tier = EncryptionTier.E2E

        coEvery { authRepository.login(email, password, tier) } returns Result.success(testUser)

        viewModel.updateEmail(email)
        viewModel.updatePassword(password)
        viewModel.selectTier(tier)

        // When
        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value is AuthUiState.Success)
        assertEquals(testUser, (viewModel.uiState.value as AuthUiState.Success).user)

        coVerify {
            authRepository.login(email, password, tier)
        }
    }

    @Test
    fun `login with invalid credentials shows error`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "wrong_password"
        val tier = EncryptionTier.E2E
        val errorMessage = "Invalid credentials"

        coEvery {
            authRepository.login(email, password, tier)
        } returns Result.failure(Exception(errorMessage))

        viewModel.updateEmail(email)
        viewModel.updatePassword(password)
        viewModel.selectTier(tier)

        // When
        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value is AuthUiState.Error)
        assertEquals(errorMessage, (viewModel.uiState.value as AuthUiState.Error).message)
    }

    @Test
    fun `login with empty email shows validation error`() = runTest {
        // Given
        viewModel.updateEmail("")
        viewModel.updatePassword("password123")
        viewModel.selectTier(EncryptionTier.E2E)

        // When
        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value is AuthUiState.Error)
        assertTrue((viewModel.uiState.value as AuthUiState.Error).message.contains("Email"))

        coVerify(exactly = 0) {
            authRepository.login(any(), any(), any())
        }
    }

    @Test
    fun `login with empty password shows validation error`() = runTest {
        // Given
        viewModel.updateEmail("test@example.com")
        viewModel.updatePassword("")
        viewModel.selectTier(EncryptionTier.E2E)

        // When
        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value is AuthUiState.Error)
        assertTrue((viewModel.uiState.value as AuthUiState.Error).message.contains("Password"))
    }

    @Test
    fun `signup with valid data succeeds`() = runTest {
        // Given
        val email = "newuser@example.com"
        val password = "password123"
        val tier = EncryptionTier.E2E
        val recoveryCodes = listOf(
            RecoveryCode("code1", false),
            RecoveryCode("code2", false),
            RecoveryCode("code3", false)
        )

        val signupResult = SignupResult(
            user = testUser,
            recoveryCodes = recoveryCodes
        )

        coEvery {
            authRepository.signup(email, password, tier)
        } returns Result.success(signupResult)

        viewModel.updateEmail(email)
        viewModel.updatePassword(password)
        viewModel.selectTier(tier)

        // When
        viewModel.signup()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value is AuthUiState.SignupSuccess)
        val state = viewModel.uiState.value as AuthUiState.SignupSuccess
        assertEquals(testUser, state.user)
        assertEquals(recoveryCodes, state.recoveryCodes)

        coVerify {
            authRepository.signup(email, password, tier)
        }
    }

    @Test
    fun `signup with existing email shows error`() = runTest {
        // Given
        val email = "existing@example.com"
        val password = "password123"
        val tier = EncryptionTier.E2E
        val errorMessage = "Email already exists"

        coEvery {
            authRepository.signup(email, password, tier)
        } returns Result.failure(Exception(errorMessage))

        viewModel.updateEmail(email)
        viewModel.updatePassword(password)
        viewModel.selectTier(tier)

        // When
        viewModel.signup()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value is AuthUiState.Error)
        assertEquals(errorMessage, (viewModel.uiState.value as AuthUiState.Error).message)
    }

    @Test
    fun `updateEmail updates email state`() = runTest {
        // Given
        val email = "test@example.com"

        // When
        viewModel.updateEmail(email)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(email, viewModel.email.value)
    }

    @Test
    fun `updatePassword updates password state`() = runTest {
        // Given
        val password = "password123"

        // When
        viewModel.updatePassword(password)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(password, viewModel.password.value)
    }

    @Test
    fun `selectTier updates selected tier state`() = runTest {
        // Given
        val tier = EncryptionTier.UCE

        // When
        viewModel.selectTier(tier)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(tier, viewModel.selectedTier.value)
    }

    @Test
    fun `clearError resets error state to idle`() = runTest {
        // Given
        coEvery {
            authRepository.login(any(), any(), any())
        } returns Result.failure(Exception("Error"))

        viewModel.updateEmail("test@example.com")
        viewModel.updatePassword("password")
        viewModel.selectTier(EncryptionTier.E2E)
        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AuthUiState.Error)

        // When
        viewModel.clearError()

        // Then
        assertTrue(viewModel.uiState.value is AuthUiState.Idle)
    }

    @Test
    fun `login shows loading state during authentication`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        val tier = EncryptionTier.E2E

        coEvery {
            authRepository.login(email, password, tier)
        } coAnswers {
            kotlinx.coroutines.delay(100)
            Result.success(testUser)
        }

        viewModel.updateEmail(email)
        viewModel.updatePassword(password)
        viewModel.selectTier(tier)

        // When
        viewModel.login()

        // Then - should be loading immediately
        assertTrue(viewModel.uiState.value is AuthUiState.Loading)

        // Wait for completion
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - should be success after completion
        assertTrue(viewModel.uiState.value is AuthUiState.Success)
    }

    @Test
    fun `isFormValid returns true when all fields are filled`() = runTest {
        // Given
        viewModel.updateEmail("test@example.com")
        viewModel.updatePassword("password123")
        viewModel.selectTier(EncryptionTier.E2E)
        testDispatcher.scheduler.advanceUntilIdle()

        // When/Then
        assertTrue(viewModel.isFormValid.value)
    }

    @Test
    fun `isFormValid returns false when email is empty`() = runTest {
        // Given
        viewModel.updateEmail("")
        viewModel.updatePassword("password123")
        viewModel.selectTier(EncryptionTier.E2E)
        testDispatcher.scheduler.advanceUntilIdle()

        // When/Then
        assertFalse(viewModel.isFormValid.value)
    }

    @Test
    fun `isFormValid returns false when password is empty`() = runTest {
        // Given
        viewModel.updateEmail("test@example.com")
        viewModel.updatePassword("")
        viewModel.selectTier(EncryptionTier.E2E)
        testDispatcher.scheduler.advanceUntilIdle()

        // When/Then
        assertFalse(viewModel.isFormValid.value)
    }
}
