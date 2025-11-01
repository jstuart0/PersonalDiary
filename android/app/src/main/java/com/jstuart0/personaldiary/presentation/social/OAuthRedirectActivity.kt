package com.jstuart0.personaldiary.presentation.social

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.jstuart0.personaldiary.data.repository.SocialRepository
import com.jstuart0.personaldiary.domain.model.SocialPlatform
import com.jstuart0.personaldiary.presentation.theme.PersonalDiaryTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Activity to handle OAuth redirects from social providers
 */
@AndroidEntryPoint
class OAuthRedirectActivity : ComponentActivity() {

    @Inject
    lateinit var socialRepository: SocialRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent?.data
        if (uri == null) {
            finish()
            return
        }

        // Parse provider from URI
        val provider = when {
            uri.toString().contains("facebook") -> SocialPlatform.FACEBOOK
            else -> {
                finish()
                return
            }
        }

        // Parse OAuth response
        val code = uri.getQueryParameter("code")
        val state = uri.getQueryParameter("state")
        val error = uri.getQueryParameter("error")
        val errorDescription = uri.getQueryParameter("error_description")

        if (error != null) {
            // Handle OAuth error
            setContent {
                PersonalDiaryTheme {
                    OAuthErrorScreen(
                        error = error,
                        errorDescription = errorDescription,
                        onClose = { finish() }
                    )
                }
            }
            return
        }

        if (code == null || state == null) {
            finish()
            return
        }

        // Handle successful OAuth callback
        setContent {
            PersonalDiaryTheme {
                OAuthCallbackScreen(
                    provider = provider,
                    code = code,
                    state = state,
                    onComplete = { success ->
                        // Return result to MainActivity
                        val resultIntent = Intent().apply {
                            putExtra("provider", provider.name)
                            putExtra("success", success)
                        }
                        setResult(if (success) RESULT_OK else RESULT_CANCELED, resultIntent)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun OAuthCallbackScreen(
    provider: SocialPlatform,
    code: String,
    state: String,
    onComplete: (Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val socialRepository = remember {
        // Will be injected by Hilt
        null as SocialRepository?
    }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                socialRepository?.handleOAuthCallback("current_user_id", provider, code, state ?: "")
                    ?.onSuccess {
                        isProcessing = false
                        onComplete(true)
                    }
                    ?.onFailure { error ->
                        isProcessing = false
                        errorMessage = error.message ?: "Failed to connect account"
                    }
            } catch (e: Exception) {
                isProcessing = false
                errorMessage = e.message ?: "An error occurred"
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isProcessing) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "Connecting ${provider.name}...",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else if (errorMessage != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Connection Failed",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = { onComplete(false) }) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
fun OAuthErrorScreen(
    error: String,
    errorDescription: String?,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Authorization Failed",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.error
                )

                Text(
                    text = error,
                    style = MaterialTheme.typography.titleMedium
                )

                if (errorDescription != null) {
                    Text(
                        text = errorDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(16.dp))

                Button(onClick = onClose) {
                    Text("Close")
                }
            }
        }
    }
}
