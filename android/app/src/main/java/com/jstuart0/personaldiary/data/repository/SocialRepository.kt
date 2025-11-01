package com.jstuart0.personaldiary.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.jstuart0.personaldiary.data.local.dao.SocialAccountDao
import com.jstuart0.personaldiary.data.local.entity.SocialAccountEntity
import com.jstuart0.personaldiary.data.remote.api.PersonalDiaryApi
import com.jstuart0.personaldiary.domain.model.SocialAccount
import com.jstuart0.personaldiary.domain.model.SocialProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for social media integration
 */
@Singleton
class SocialRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: PersonalDiaryApi,
    private val socialAccountDao: SocialAccountDao
) {

    companion object {
        private const val FACEBOOK_AUTH_URL = "https://www.facebook.com/v18.0/dialog/oauth"
        private const val FACEBOOK_REDIRECT_URI = "personaldiary://oauth/facebook"
        private const val FACEBOOK_SCOPE = "public_profile,email,user_posts,user_photos"
    }

    /**
     * Get all connected social accounts
     */
    fun getSocialAccounts(): Flow<List<SocialAccount>> {
        return socialAccountDao.getAllFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Get social account by provider
     */
    suspend fun getSocialAccount(provider: SocialProvider): SocialAccount? {
        return socialAccountDao.getByProvider(provider.name)?.toDomain()
    }

    /**
     * Initiate Facebook OAuth flow
     */
    fun startFacebookAuth(facebookAppId: String) {
        val authUrl = buildFacebookAuthUrl(facebookAppId)
        launchCustomTab(authUrl)
    }

    /**
     * Build Facebook OAuth URL
     */
    private fun buildFacebookAuthUrl(appId: String): String {
        val redirectUri = URLEncoder.encode(FACEBOOK_REDIRECT_URI, "UTF-8")
        val scope = URLEncoder.encode(FACEBOOK_SCOPE, "UTF-8")
        val state = generateRandomState()

        return "$FACEBOOK_AUTH_URL?" +
                "client_id=$appId&" +
                "redirect_uri=$redirectUri&" +
                "scope=$scope&" +
                "state=$state&" +
                "response_type=code"
    }

    /**
     * Handle OAuth callback
     */
    suspend fun handleOAuthCallback(
        provider: SocialProvider,
        code: String,
        state: String
    ): Result<SocialAccount> {
        return try {
            // Exchange code for access token via backend
            val response = api.exchangeSocialToken(
                provider = provider.name.lowercase(),
                code = code,
                redirectUri = getRedirectUri(provider)
            )

            if (!response.isSuccessful || response.body() == null) {
                return Result.failure(Exception("Failed to exchange token"))
            }

            val tokenData = response.body()!!

            // Save social account
            val account = SocialAccount(
                accountId = tokenData.accountId,
                userId = tokenData.userId,
                provider = provider,
                providerUserId = tokenData.providerUserId,
                displayName = tokenData.displayName,
                email = tokenData.email,
                profilePictureUrl = tokenData.profilePictureUrl,
                accessToken = tokenData.accessToken,
                refreshToken = tokenData.refreshToken,
                expiresAt = System.currentTimeMillis() + (tokenData.expiresIn * 1000),
                connectedAt = System.currentTimeMillis()
            )

            socialAccountDao.insert(SocialAccountEntity.fromDomain(account))

            Result.success(account)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Disconnect social account
     */
    suspend fun disconnectAccount(accountId: String): Result<Unit> {
        return try {
            // Delete from server
            val response = api.disconnectSocialAccount(accountId)
            if (!response.isSuccessful) {
                return Result.failure(Exception("Failed to disconnect account"))
            }

            // Delete from local database
            val account = socialAccountDao.getById(accountId)
            if (account != null) {
                socialAccountDao.delete(account)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Share entry to Facebook
     */
    suspend fun shareToFacebook(
        accountId: String,
        message: String,
        link: String? = null
    ): Result<String> {
        return try {
            val account = socialAccountDao.getById(accountId)
                ?: return Result.failure(Exception("Account not found"))

            val response = api.shareToSocial(
                provider = SocialProvider.FACEBOOK.name.lowercase(),
                accountId = accountId,
                message = message,
                link = link
            )

            if (!response.isSuccessful || response.body() == null) {
                return Result.failure(Exception("Failed to share"))
            }

            val postId = response.body()!!.postId
            Result.success(postId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Import posts from Facebook
     */
    suspend fun importFromFacebook(
        accountId: String,
        since: Long? = null,
        limit: Int = 100
    ): Result<List<ImportedPost>> {
        return try {
            val response = api.importFromSocial(
                provider = SocialProvider.FACEBOOK.name.lowercase(),
                accountId = accountId,
                since = since,
                limit = limit
            )

            if (!response.isSuccessful || response.body() == null) {
                return Result.failure(Exception("Failed to import posts"))
            }

            val posts = response.body()!!.posts.map { post ->
                ImportedPost(
                    postId = post.postId,
                    message = post.message,
                    createdTime = post.createdTime,
                    permalink = post.permalink,
                    attachments = post.attachments
                )
            }

            Result.success(posts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Launch Chrome Custom Tab for OAuth
     */
    private fun launchCustomTab(url: String) {
        val intent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setUrlBarHidingEnabled(true)
            .build()

        intent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.launchUrl(context, Uri.parse(url))
    }

    /**
     * Get redirect URI for provider
     */
    private fun getRedirectUri(provider: SocialProvider): String {
        return when (provider) {
            SocialProvider.FACEBOOK -> FACEBOOK_REDIRECT_URI
            else -> "personaldiary://oauth/${provider.name.lowercase()}"
        }
    }

    /**
     * Generate random state for OAuth
     */
    private fun generateRandomState(): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..32)
            .map { allowedChars.random() }
            .joinToString("")
    }
}

/**
 * Imported post from social media
 */
data class ImportedPost(
    val postId: String,
    val message: String?,
    val createdTime: Long,
    val permalink: String?,
    val attachments: List<PostAttachment>
)

/**
 * Post attachment (photo, video, etc.)
 */
data class PostAttachment(
    val type: String,
    val url: String,
    val width: Int?,
    val height: Int?
)
