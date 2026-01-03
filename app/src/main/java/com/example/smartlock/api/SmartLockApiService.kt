package com.example.smartlock.api

import com.example.smartlock.model.auth.*
import com.example.smartlock.model.common.ApiResponse
import com.example.smartlock.model.door.*
import com.example.smartlock.model.doorshare.*
import com.example.smartlock.model.entity.*
import com.example.smartlock.model.iccard.*
import com.example.smartlock.model.passcode.*
import com.example.smartlock.model.userprofile.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface SmartLockApiService {
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<Unit>>

    @POST("api/auth/verify-register-otp")
    suspend fun verifyRegisterOtp(@Body request: VerifyOtpRequest): Response<ApiResponse<Unit>>

    @POST("api/auth/resend-register-otp")
    suspend fun resendRegisterOtp(@Body request: ForgotPasswordRequest): Response<ApiResponse<Unit>>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<AuthResponse>>

    @POST("api/auth/refresh-token")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<ApiResponse<AuthResponse>>

    @POST("api/auth/logout")
    suspend fun logout(@Body request: LogoutRequest): Response<ApiResponse<Unit>>

    @POST("api/auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<ApiResponse<Unit>>

    @POST("api/auth/forgot-password/send-otp")
    suspend fun sendForgotPasswordOtp(@Body request: ForgotPasswordRequest): Response<ApiResponse<Unit>>

    @POST("api/auth/forgot-password/verify")
    suspend fun verifyForgotPasswordOtp(@Body request: VerifyOtpRequest): Response<ApiResponse<Unit>>

    @POST("api/auth/link-oauth")
    suspend fun linkOAuth(@Body request: LinkOAuthRequest): Response<ApiResponse<Unit>>

    @POST("api/auth/unlink-oauth")
    suspend fun unlinkOAuth(@Body request: UnlinkOAuthRequest): Response<ApiResponse<Unit>>

    @GET("api/users/me")
    suspend fun getUserProfile(): Response<ApiResponse<User>>

    @PUT("api/users/me/name")
    suspend fun updateName(@Body request: UpdateNameRequest): Response<ApiResponse<Unit>>

    @Multipart
    @PUT("api/users/me/avatar")
    suspend fun updateAvatar(
        @Query("isRandom") isRandom: Boolean,
        @Part file: MultipartBody.Part?
    ): Response<ApiResponse<Unit>>

    @PUT("api/users/me/phone-number")
    suspend fun updatePhoneNumber(@Body request: UpdatePhoneRequest): Response<ApiResponse<Unit>>

    @PUT("api/users/me/date-of-birth")
    suspend fun updateDateOfBirth(@Body request: UpdateDateOfBirthRequest): Response<ApiResponse<Unit>>

    @GET("api/doors")
    suspend fun getDoors(): Response<List<Door>>

    @POST("api/doors")
    suspend fun createDoor(@Body request: CreateDoorRequest): Response<ApiResponse<String>>

    @GET("api/doors/{doorId}")
    suspend fun getDoor(@Path("doorId") doorId: String): Response<Door>

    @PUT("api/doors/{doorId}")
    suspend fun updateDoor(
        @Path("doorId") doorId: String,
        @Body request: UpdateDoorRequest
    ): Response<ApiResponse<Unit>>

    @DELETE("api/doors/{doorId}")
    suspend fun deleteDoor(@Path("doorId") doorId: String): Response<ApiResponse<Unit>>

    @PUT("api/doors/{doorId}/doorcode")
    suspend fun updateDoorCode(
        @Path("doorId") doorId: String,
        @Body request: UpdateDoorCodeRequest
    ): Response<ApiResponse<Unit>>

    @POST("api/doors/{doorId}/lock")
    suspend fun lockDoor(@Path("doorId") doorId: String): Response<ApiResponse<Unit>>

    @POST("api/doors/{doorId}/unlock")
    suspend fun unlockDoor(@Path("doorId") doorId: String): Response<ApiResponse<Unit>>

    @POST("api/doors/{doorId}/sync")
    suspend fun syncDoorStatus(@Path("doorId") doorId: String): Response<ApiResponse<Unit>>

    @GET("api/doors/{doorId}/records")
    suspend fun getDoorRecords(@Path("doorId") doorId: String): Response<List<DoorRecord>>

    @GET("api/doors/{doorId}/records/{recordId}")
    suspend fun getDoorRecord(
        @Path("doorId") doorId: String,
        @Path("recordId") recordId: String
    ): Response<DoorRecord>

    @GET("api/doors/{doorId}/shares")
    suspend fun getDoorShares(@Path("doorId") doorId: String): Response<List<DoorShare>>

    @POST("api/doors/{doorId}/shares")
    suspend fun shareDoor(
        @Path("doorId") doorId: String,
        @Body request: ShareDoorRequest
    ): Response<ApiResponse<Unit>>

    @PUT("api/doors/{doorId}/shares/{userId}")
    suspend fun updateShare(
        @Path("doorId") doorId: String,
        @Path("userId") userId: String,
        @Body request: UpdateShareRequest
    ): Response<ApiResponse<Unit>>

    @DELETE("api/doors/{doorId}/shares/{userId}")
    suspend fun revokeShare(
        @Path("doorId") doorId: String,
        @Path("userId") userId: String
    ): Response<ApiResponse<Unit>>

    @GET("api/doors/{doorId}/passcodes")
    suspend fun getPasscodes(@Path("doorId") doorId: String): Response<List<Passcode>>

    @POST("api/doors/{doorId}/passcodes/add")
    suspend fun addPasscode(
        @Path("doorId") doorId: String,
        @Body request: AddPasscodeRequest
    ): Response<ApiResponse<Unit>>

    @POST("api/doors/{doorId}/passcodes/update")
    suspend fun updatePasscode(
        @Path("doorId") doorId: String,
        @Body request: UpdatePasscodeRequest
    ): Response<ApiResponse<Unit>>

    @POST("api/doors/{doorId}/passcodes/delete")
    suspend fun deletePasscode(
        @Path("doorId") doorId: String,
        @Body request: DeletePasscodeRequest
    ): Response<ApiResponse<Unit>>

    @POST("api/doors/{doorId}/passcodes/request-sync")
    suspend fun syncPasscodes(@Path("doorId") doorId: String): Response<ApiResponse<Unit>>

    @GET("api/doors/{doorId}/iccards")
    suspend fun getICCards(@Path("doorId") doorId: String): Response<List<ICCard>>

    @POST("api/doors/{doorId}/iccards/add")
    suspend fun addICCard(
        @Path("doorId") doorId: String,
        @Body request: AddICCardRequest
    ): Response<Boolean>

    @POST("api/doors/{doorId}/iccards/delete")
    suspend fun deleteICCard(
        @Path("doorId") doorId: String,
        @Body request: DeleteICCardRequest
    ): Response<ApiResponse<Unit>>

    @POST("api/doors/{doorId}/iccards/start-swipe-add")
    suspend fun startSwipeAdd(@Path("doorId") doorId: String): Response<Boolean>

    @POST("api/doors/{doorId}/iccards/request-sync")
    suspend fun syncICCards(@Path("doorId") doorId: String): Response<ApiResponse<Unit>>
}