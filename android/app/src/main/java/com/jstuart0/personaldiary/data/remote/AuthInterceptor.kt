package com.jstuart0.personaldiary.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * OkHttp interceptor to add JWT token to requests
 * Handles token refresh on 401 Unauthorized
 */
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip auth for login/signup/refresh endpoints
        if (originalRequest.url.encodedPath.contains("/auth/login") ||
            originalRequest.url.encodedPath.contains("/auth/signup") ||
            originalRequest.url.encodedPath.contains("/auth/refresh")
        ) {
            return chain.proceed(originalRequest)
        }

        // Add access token to request
        val accessToken = tokenManager.getAccessToken()
        val request = if (accessToken != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
        } else {
            originalRequest
        }

        var response = chain.proceed(request)

        // Handle 401 Unauthorized - token expired
        if (response.code == 401 && accessToken != null) {
            response.close()

            // Try to refresh token
            val newAccessToken = runBlocking {
                refreshToken()
            }

            // Retry request with new token
            if (newAccessToken != null) {
                val newRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer $newAccessToken")
                    .build()
                response = chain.proceed(newRequest)
            }
        }

        return response
    }

    /**
     * Attempt to refresh the access token
     * Returns new access token if successful, null otherwise
     */
    private suspend fun refreshToken(): String? {
        return try {
            val refreshToken = tokenManager.getRefreshToken() ?: return null

            // Note: This creates a circular dependency issue
            // In production, use a separate OkHttpClient without this interceptor
            // for refresh token requests. For now, this is a simplified version.

            // TODO: Implement proper token refresh with separate client
            // For now, return null to force re-login
            null
        } catch (e: Exception) {
            null
        }
    }
}
