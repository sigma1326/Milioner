package com.ads.milioner.Model.network

import androidx.annotation.Keep
import com.ads.milioner.Model.network.model.*
import io.reactivex.Observable
import retrofit2.Response
import retrofit2.http.*

@Keep
interface ApiService {

    @POST("/api/users/register/")
    @FormUrlEncoded
    fun register(@Field("phone") phone: String): Observable<Response<RegisterResponse>>

    @POST("/api/users/login/")
    @FormUrlEncoded
    fun login(@Field("phone") phone: String, @Field("code") code: String): Observable<Response<LoginSuccess>>

    @GET("/api/users/me")
    fun me(@Header("Authorization") authorization: String): Observable<Response<MeResponse>>

    @POST("/api/users/refresh/")
    @FormUrlEncoded
    fun refresh(@Field("token") token: String): Observable<Response<RefreshResponse>>


    @POST("/api/fin/charge-req/")
    @FormUrlEncoded
    fun charge(@Header("Authorization") authorization: String, @Field("phone") phone: String): Observable<Response<ChargeResponse>>


    @GET("/api/check/ip-ad/")
    fun checkIpForAds(): Observable<Response<CheckAdsResponse>>

    @POST("/api/fin/ads/")
    @FormUrlEncoded
    fun ads(
        @Header("Authorization") authorization: String,
        @Field("gid") gid: String, @Field("hash") hash: String,
        @Field("timestamp") timestamp: String,
        @Field("reward") reward: String
    ): Observable<Response<AdsResponse>>

}