package com.jstuart0.personaldiary.presentation.auth

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jstuart0.personaldiary.domain.model.EncryptionTier
import com.jstuart0.personaldiary.domain.model.User
import com.jstuart0.personaldiary.presentation.theme.PersonalDiaryTheme
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var emailState: MutableStateFlow<String>
    private lateinit var passwordState: MutableStateFlow<String>
    private lateinit var selectedTierState: MutableStateFlow<EncryptionTier?>
    private lateinit var uiState: MutableStateFlow<AuthUiState>
    private lateinit var isFormValidState: MutableStateFlow<Boolean>

    private var updateEmailCalled = false
    private var updatePasswordCalled = false
    private var selectTierCalled = false
    private var loginCalled = false
    private var navigateToSignupCalled = false

    @Before
    fun setup() {
        emailState = MutableStateFlow("")
        passwordState = MutableStateFlow("")
        selectedTierState = MutableStateFlow(null)
        uiState = MutableStateFlow(AuthUiState.Idle)
        isFormValidState = MutableStateFlow(false)

        updateEmailCalled = false
        updatePasswordCalled = false
        selectTierCalled = false
        loginCalled = false
        navigateToSignupCalled = false
    }

    @Test
    fun loginScreen_displaysAllElements() {
        composeTestRule.setContent {
            PersonalDiaryTheme {
                LoginScreen(
                    email = emailState.value,
                    password = passwordState.value,
                    selectedTier = selectedTierState.value,
                    uiState = uiState.value,
                    isFormValid = isFormValidState.value,
                    onEmailChange = { updateEmailCalled = true },
                    onPasswordChange = { updatePasswordCalled = true },
                    onTierSelect = { selectTierCalled = true },
                    onLogin = { loginCalled = true },
                    onNavigateToSignup = { navigateToSignupCalled = true },
                    onClearError = {}
                )
            }
        }

        // Verify all UI elements are displayed
        composeTestRule.onNodeWithText("Welcome Back").assertIsDisplayed()
        composeTestRule.onNodeWithText("Email").assertIsDisplayed()
        composeTestRule.onNodeWithText("Password").assertIsDisplayed()
        composeTestRule.onNodeWithText("Login").assertIsDisplayed()
        composeTestRule.onNodeWithText("Don't have an account? Sign up").assertIsDisplayed()
    }

    @Test
    fun loginScreen_emailInput_callsUpdateEmail() {
        composeTestRule.setContent {
            PersonalDiaryTheme {
                LoginScreen(
                    email = emailState.value,
                    password = passwordState.value,
                    selectedTier = selectedTierState.value,
                    uiState = uiState.value,
                    isFormValid = isFormValidState.value,
                    onEmailChange = { updateEmailCalled = true },
                    onPasswordChange = { },
                    onTierSelect = { },
                    onLogin = { },
                    onNavigateToSignup = { },
                    onClearError = {}
                )
            }
        }

        // Type in email field
        composeTestRule.onNodeWithText("Email").performTextInput("test@example.com")

        // Verify callback was called
        assert(updateEmailCalled)
    }

    @Test
    fun loginScreen_passwordInput_callsUpdatePassword() {
        composeTestRule.setContent {
            PersonalDiaryTheme {
                LoginScreen(
                    email = emailState.value,
                    password = passwordState.value,
                    selectedTier = selectedTierState.value,
                    uiState = uiState.value,
                    isFormValid = isFormValidState.value,
                    onEmailChange = { },
                    onPasswordChange = { updatePasswordCalled = true },
                    onTierSelect = { },
                    onLogin = { },
                    onNavigateToSignup = { },
                    onClearError = {}
                )
            }
        }

        // Type in password field
        composeTestRule.onNodeWithText("Password").performTextInput("password123")

        // Verify callback was called
        assert(updatePasswordCalled)
    }

    @Test
    fun loginScreen_loginButton_disabled_whenFormInvalid() {
        isFormValidState.value = false

        composeTestRule.setContent {
            PersonalDiaryTheme {
                LoginScreen(
                    email = emailState.value,
                    password = passwordState.value,
                    selectedTier = selectedTierState.value,
                    uiState = uiState.value,
                    isFormValid = isFormValidState.value,
                    onEmailChange = { },
                    onPasswordChange = { },
                    onTierSelect = { },
                    onLogin = { loginCalled = true },
                    onNavigateToSignup = { },
                    onClearError = {}
                )
            }
        }

        // Verify login button is disabled
        composeTestRule.onNodeWithText("Login").assertIsNotEnabled()

        // Try to click (should not work)
        composeTestRule.onNodeWithText("Login").performClick()
        assert(!loginCalled)
    }

    @Test
    fun loginScreen_loginButton_enabled_whenFormValid() {
        emailState.value = "test@example.com"
        passwordState.value = "password123"
        selectedTierState.value = EncryptionTier.E2E
        isFormValidState.value = true

        composeTestRule.setContent {
            PersonalDiaryTheme {
                LoginScreen(
                    email = emailState.value,
                    password = passwordState.value,
                    selectedTier = selectedTierState.value,
                    uiState = uiState.value,
                    isFormValid = isFormValidState.value,
                    onEmailChange = { },
                    onPasswordChange = { },
                    onTierSelect = { },
                    onLogin = { loginCalled = true },
                    onNavigateToSignup = { },
                    onClearError = {}
                )
            }
        }

        // Verify login button is enabled
        composeTestRule.onNodeWithText("Login").assertIsEnabled()

        // Click login button
        composeTestRule.onNodeWithText("Login").performClick()
        assert(loginCalled)
    }

    @Test
    fun loginScreen_showsLoadingIndicator_whenLoading() {
        uiState.value = AuthUiState.Loading

        composeTestRule.setContent {
            PersonalDiaryTheme {
                LoginScreen(
                    email = emailState.value,
                    password = passwordState.value,
                    selectedTier = selectedTierState.value,
                    uiState = uiState.value,
                    isFormValid = isFormValidState.value,
                    onEmailChange = { },
                    onPasswordChange = { },
                    onTierSelect = { },
                    onLogin = { },
                    onNavigateToSignup = { },
                    onClearError = {}
                )
            }
        }

        // Verify loading indicator is shown
        composeTestRule.onNode(hasTestTag("loading")).assertExists()

        // Verify login button shows "Logging in..."
        composeTestRule.onNodeWithText("Logging in...").assertIsDisplayed()
    }

    @Test
    fun loginScreen_showsError_whenErrorState() {
        uiState.value = AuthUiState.Error("Invalid credentials")

        composeTestRule.setContent {
            PersonalDiaryTheme {
                LoginScreen(
                    email = emailState.value,
                    password = passwordState.value,
                    selectedTier = selectedTierState.value,
                    uiState = uiState.value,
                    isFormValid = isFormValidState.value,
                    onEmailChange = { },
                    onPasswordChange = { },
                    onTierSelect = { },
                    onLogin = { },
                    onNavigateToSignup = { },
                    onClearError = {}
                )
            }
        }

        // Verify error message is shown
        composeTestRule.onNodeWithText("Invalid credentials").assertIsDisplayed()
    }

    @Test
    fun loginScreen_signupLink_callsNavigateToSignup() {
        composeTestRule.setContent {
            PersonalDiaryTheme {
                LoginScreen(
                    email = emailState.value,
                    password = passwordState.value,
                    selectedTier = selectedTierState.value,
                    uiState = uiState.value,
                    isFormValid = isFormValidState.value,
                    onEmailChange = { },
                    onPasswordChange = { },
                    onTierSelect = { },
                    onLogin = { },
                    onNavigateToSignup = { navigateToSignupCalled = true },
                    onClearError = {}
                )
            }
        }

        // Click on signup link
        composeTestRule.onNodeWithText("Don't have an account? Sign up").performClick()

        // Verify callback was called
        assert(navigateToSignupCalled)
    }

    @Test
    fun tierSelectionScreen_showsAllTiers() {
        composeTestRule.setContent {
            PersonalDiaryTheme {
                TierSelectionScreen(
                    selectedTier = selectedTierState.value,
                    onTierSelect = { },
                    onContinue = { }
                )
            }
        }

        // Verify all encryption tiers are displayed
        composeTestRule.onNodeWithText("End-to-End Encrypted").assertIsDisplayed()
        composeTestRule.onNodeWithText("User-Controlled Encryption").assertIsDisplayed()
        composeTestRule.onNodeWithText("Standard").assertIsDisplayed()
    }

    @Test
    fun tierSelectionScreen_selectingTier_callsCallback() {
        composeTestRule.setContent {
            PersonalDiaryTheme {
                TierSelectionScreen(
                    selectedTier = selectedTierState.value,
                    onTierSelect = { selectTierCalled = true },
                    onContinue = { }
                )
            }
        }

        // Click on E2E tier
        composeTestRule.onNodeWithText("End-to-End Encrypted").performClick()

        // Verify callback was called
        assert(selectTierCalled)
    }

    @Test
    fun tierSelectionScreen_continueButton_disabled_whenNoTierSelected() {
        selectedTierState.value = null

        composeTestRule.setContent {
            PersonalDiaryTheme {
                TierSelectionScreen(
                    selectedTier = selectedTierState.value,
                    onTierSelect = { },
                    onContinue = { }
                )
            }
        }

        // Verify continue button is disabled
        composeTestRule.onNodeWithText("Continue").assertIsNotEnabled()
    }

    @Test
    fun tierSelectionScreen_continueButton_enabled_whenTierSelected() {
        selectedTierState.value = EncryptionTier.E2E

        var continueCalled = false

        composeTestRule.setContent {
            PersonalDiaryTheme {
                TierSelectionScreen(
                    selectedTier = selectedTierState.value,
                    onTierSelect = { },
                    onContinue = { continueCalled = true }
                )
            }
        }

        // Verify continue button is enabled
        composeTestRule.onNodeWithText("Continue").assertIsEnabled()

        // Click continue
        composeTestRule.onNodeWithText("Continue").performClick()
        assert(continueCalled)
    }
}
