package com.jstuart0.personaldiary.data.remote.api

import com.jstuart0.personaldiary.data.remote.model.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API interface for Personal Diary backend
 */
interface PersonalDiaryApi {

    // ==================== Authentication ====================

    @POST("auth/signup")
    suspend fun signup(@Body request: SignupRequest): Response<SignupResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<RefreshTokenResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

    @POST("auth/password-reset")
    suspend fun requestPasswordReset(@Body request: PasswordResetRequest): Response<Unit>

    @POST("auth/password-reset/confirm")
    suspend fun confirmPasswordReset(@Body request: PasswordResetConfirmRequest): Response<Unit>

    @POST("auth/recovery-code/verify")
    suspend fun verifyRecoveryCode(@Body request: RecoveryCodeVerifyRequest): Response<LoginResponse>

    // ==================== User Profile ====================

    @GET("user/profile")
    suspend fun getUserProfile(): Response<LoginResponse>

    @DELETE("user/account")
    suspend fun deleteAccount(): Response<Unit>

    // ==================== Entries ====================

    @GET("entries")
    suspend fun getEntries(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("source") source: String? = null
    ): Response<List<EntryDto>>

    @GET("entries/{entry_id}")
    suspend fun getEntry(@Path("entry_id") entryId: String): Response<EntryDto>

    @POST("entries")
    suspend fun createEntry(@Body request: CreateEntryRequest): Response<EntryDto>

    @PUT("entries/{entry_id}")
    suspend fun updateEntry(
        @Path("entry_id") entryId: String,
        @Body request: UpdateEntryRequest
    ): Response<EntryDto>

    @DELETE("entries/{entry_id}")
    suspend fun deleteEntry(@Path("entry_id") entryId: String): Response<Unit>

    @POST("entries/sync")
    suspend fun syncEntries(@Body request: SyncEntriesRequest): Response<SyncEntriesResponse>

    // ==================== Search (UCE only) ====================

    @POST("entries/search")
    suspend fun search(@Body request: SearchRequest): Response<SearchResponse>

    // ==================== Media ====================

    @GET("media/{media_id}")
    suspend fun getMedia(@Path("media_id") mediaId: String): Response<MediaDto>

    @POST("media/upload-url")
    suspend fun getUploadUrl(
        @Query("entry_id") entryId: String,
        @Query("mime_type") mimeType: String,
        @Query("file_size") fileSize: Long
    ): Response<MediaUploadResponse>

    @PUT
    suspend fun uploadMedia(
        @Url uploadUrl: String,
        @Body file: MultipartBody
    ): Response<Unit>

    @DELETE("media/{media_id}")
    suspend fun deleteMedia(@Path("media_id") mediaId: String): Response<Unit>

    @GET("media/{media_id}/download")
    @Streaming
    suspend fun downloadMedia(@Path("media_id") mediaId: String): Response<ResponseBody>

    // ==================== Social Accounts ====================

    @GET("social/accounts")
    suspend fun getSocialAccounts(): Response<List<SocialAccountDto>>

    @POST("social/connect")
    suspend fun connectSocialAccount(@Body request: ConnectSocialAccountRequest): Response<SocialAccountDto>

    @DELETE("social/accounts/{account_id}")
    suspend fun disconnectSocialAccount(@Path("account_id") accountId: String): Response<Unit>

    @POST("social/accounts/{account_id}/sync")
    suspend fun syncSocialAccount(@Path("account_id") accountId: String): Response<List<EntryDto>>

    // ==================== Facebook Integration ====================

    @POST("social/facebook/import")
    suspend fun importFacebookPosts(
        @Body request: Map<String, String> // Contains access_token
    ): Response<List<EntryDto>>

    @POST("social/facebook/share")
    suspend fun shareToFacebook(
        @Body request: Map<String, String> // Contains entry_id and any custom message
    ): Response<Map<String, String>> // Returns post_url

    // ==================== Social OAuth ====================

    @POST("social/oauth/exchange")
    suspend fun exchangeSocialToken(
        @Query("provider") provider: String,
        @Query("code") code: String,
        @Query("redirect_uri") redirectUri: String
    ): Response<SocialTokenResponse>

    @POST("social/share")
    suspend fun shareToSocial(
        @Query("provider") provider: String,
        @Query("account_id") accountId: String,
        @Query("message") message: String,
        @Query("link") link: String? = null
    ): Response<SocialShareResponse>

    @POST("social/import")
    suspend fun importFromSocial(
        @Query("provider") provider: String,
        @Query("account_id") accountId: String,
        @Query("since") since: Long? = null,
        @Query("limit") limit: Int = 100
    ): Response<SocialImportResponse>
}
