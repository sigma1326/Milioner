package com.ads.milioner.Model.network

import com.ads.milioner.Model.network.model.*
import io.reactivex.Observable
import retrofit2.Response
import retrofit2.http.*


interface ApiService {

    @POST("/users/register/")
    @FormUrlEncoded
    fun register(@Field("phone") phone: String): Observable<Response<RegisterResponse>>

    @POST("/users/login/")
    @FormUrlEncoded
    fun login(@Field("phone") phone: String, @Field("code") code: String): Observable<Response<LoginSuccess>>

    @GET("/users/me")
    fun me(@Header("Authorization") authorization: String): Observable<Response<MeResponse>>

    @POST("/users/refresh/")
    @FormUrlEncoded
    fun refresh(@Field("token") token: String): Observable<Response<RefreshResponse>>


    @GET("/api/charge/")
    fun charge(@Header("Authorization") authorization: String): Observable<Response<ChargeResponse>>

    @GET("/api/check-ip/")
    fun checkIP(): Observable<Response<CheckIPResponse>>


    @POST("/api/ads/")
    @FormUrlEncoded
    fun ads(
        @Header("Authorization") authorization: String
        , @Field("gid") gid: String, @Field("hash") hash: String,
        @Field("timestamp") timestamp: String
    ): Observable<Response<AdsResponse>>

}