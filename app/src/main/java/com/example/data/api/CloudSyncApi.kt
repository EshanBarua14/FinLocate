package com.example.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

// --- DTO Data Structures ---

data class CloudBackupRequest(
    val passcodeHash: String,
    val encryptedData: String
)

data class CloudRestoreRequest(
    val passcodeHash: String
)

data class CloudBackupResponse(
    val success: Boolean,
    val message: String? = null,
    val timestamp: Long? = null
)

data class CloudRestoreResponse(
    val success: Boolean,
    val encryptedData: String? = null,
    val updatedAt: Long? = null
)

data class CloudStatusResponse(
    val status: String,
    val service: String,
    val version: String,
    val timestamp: String
)

// --- Auth and REST DTOs ---

data class AuthRegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val taxProfile: String? = "USA"
)

data class AuthLoginRequest(
    val email: String,
    val password: String
)

data class AuthUser(
    val id: Int,
    val username: String,
    val email: String,
    val taxProfile: String? = null
)

data class AuthResponse(
    val success: Boolean,
    val message: String? = null,
    val error: String? = null,
    val token: String? = null,
    val user: AuthUser? = null
)

data class ExpenseSyncDto(
    val id: String,
    val amount: Double,
    val type: String,
    val merchant: String,
    val categoryName: String,
    val accountName: String,
    val notes: String,
    val timestamp: Long,
    val isTaxDeductible: Boolean,
    val taxRate: Double
)

data class AccountSyncDto(
    val id: String,
    val name: String,
    val type: String,
    val balance: Double,
    val currency: String,
    val provider: String,
    val updatedAt: Long
)

data class ExpensesResponseDto(
    val success: Boolean,
    val count: Int,
    val expenses: List<ExpenseSyncDto>? = null,
    val error: String? = null
)

data class AccountsResponseDto(
    val success: Boolean,
    val count: Int,
    val accounts: List<AccountSyncDto>? = null,
    val error: String? = null
)

data class SyncResponseDto(
    val success: Boolean,
    val count: Int? = null,
    val message: String? = null,
    val error: String? = null
)

data class BudgetSyncDto(
    val id: String? = null,
    val categoryId: Long,
    val categoryName: String,
    val amount: Double,
    val month: String
)

data class NotificationSyncDto(
    val id: String,
    val userEmail: String,
    val title: String,
    val message: String,
    val categoryName: String,
    val timestamp: Long,
    val isRead: Boolean
)

data class NotificationsResponseDto(
    val success: Boolean,
    val count: Int,
    val notifications: List<NotificationSyncDto>? = null,
    val error: String? = null
)

// --- Retrofit API Service ---

interface CloudSyncService {
    @GET
    suspend fun getStatus(@Url url: String): CloudStatusResponse

    @POST
    suspend fun uploadBackup(@Url url: String, @Body request: CloudBackupRequest): CloudBackupResponse

    @POST
    suspend fun downloadBackup(@Url url: String, @Body request: CloudRestoreRequest): CloudRestoreResponse

    // --- Modern REST and Auth Endpoints ---
    @POST
    suspend fun registerUser(@Url url: String, @Body request: AuthRegisterRequest): AuthResponse

    @POST
    suspend fun loginUser(@Url url: String, @Body request: AuthLoginRequest): AuthResponse

    @GET
    suspend fun getExpenses(
        @Url url: String,
        @Header("Authorization") authHeader: String
    ): ExpensesResponseDto

    @GET
    suspend fun searchExpenses(
        @Url url: String,
        @Header("Authorization") authHeader: String,
        @retrofit2.http.Query("startDate") startDate: String?,
        @retrofit2.http.Query("endDate") endDate: String?,
        @retrofit2.http.Query("categoryName") categoryName: String?,
        @retrofit2.http.Query("keyword") keyword: String?
    ): ExpensesResponseDto

    @POST
    suspend fun syncExpenses(
        @Url url: String,
        @Header("Authorization") authHeader: String,
        @Body expenses: List<ExpenseSyncDto>
    ): SyncResponseDto

    @GET
    suspend fun getAccounts(
        @Url url: String,
        @Header("Authorization") authHeader: String
    ): AccountsResponseDto

    @POST
    suspend fun syncAccounts(
        @Url url: String,
        @Header("Authorization") authHeader: String,
        @Body accounts: List<AccountSyncDto>
    ): SyncResponseDto

    @POST
    suspend fun syncBudgets(
        @Url url: String,
        @Header("Authorization") authHeader: String,
        @Body budgets: List<BudgetSyncDto>
    ): SyncResponseDto

    @GET
    suspend fun getNotifications(
        @Url url: String,
        @Header("Authorization") authHeader: String
    ): NotificationsResponseDto
}

// --- Dynamic API Client ---

object CloudSyncClient {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    // Base URL is just a placeholder because we use dynamic @Url routing
    private val retrofit = Retrofit.Builder()
        .baseUrl("http://localhost/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val service: CloudSyncService by lazy {
        retrofit.create(CloudSyncService::class.java)
    }
}
