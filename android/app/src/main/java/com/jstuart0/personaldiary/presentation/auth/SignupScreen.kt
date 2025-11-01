package com.jstuart0.personaldiary.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jstuart0.personaldiary.domain.model.EncryptionTier

/**
 * Signup screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    tier: String = "E2E",
    viewModel: AuthViewModel = hiltViewModel(),
    onSignupSuccess: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val encryptionTier = remember { EncryptionTier.valueOf(tier) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val passwordMatch = password == confirmPassword
    val passwordValid = password.length >= 8
    val canSubmit = email.isNotBlank() && passwordValid && passwordMatch

    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthUiState.Authenticated -> onSignupSuccess()
            is AuthUiState.RecoveryCodesRequired -> {
                // Show recovery codes dialog
                // For now, just acknowledge and continue
                viewModel.acknowledgeRecoveryCodes()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Account") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Sign Up",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Security: ${if (encryptionTier == EncryptionTier.E2E) "Maximum (E2E)" else "Balanced (UCE)"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                enabled = uiState !is AuthUiState.Loading
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                singleLine = true,
                enabled = uiState !is AuthUiState.Loading,
                supportingText = {
                    Text(
                        text = if (!passwordValid && password.isNotEmpty()) {
                            "Password must be at least 8 characters"
                        } else {
                            "Minimum 8 characters"
                        },
                        color = if (!passwordValid && password.isNotEmpty()) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (confirmPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (canSubmit) {
                            viewModel.signup(email, password, encryptionTier)
                        }
                    }
                ),
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                singleLine = true,
                enabled = uiState !is AuthUiState.Loading,
                isError = !passwordMatch && confirmPassword.isNotEmpty(),
                supportingText = if (!passwordMatch && confirmPassword.isNotEmpty()) {
                    { Text("Passwords do not match", color = MaterialTheme.colorScheme.error) }
                } else null
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.signup(email, password, encryptionTier) },
                modifier = Modifier.fillMaxWidth(),
                enabled = canSubmit && uiState !is AuthUiState.Loading
            ) {
                if (uiState is AuthUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Create Account")
                }
            }

            if (encryptionTier == EncryptionTier.E2E) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "⚠ You will receive recovery codes. Save them safely!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (uiState is AuthUiState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = (uiState as AuthUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
