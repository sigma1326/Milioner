package com.ads.milioner.Model.network

import android.annotation.SuppressLint
import androidx.annotation.Keep
import com.ads.milioner.Model.AppManager
import com.ads.milioner.Model.database.DataBaseRepositoryImpl
import com.ads.milioner.Model.database.model.User
import com.ads.milioner.Model.network.model.*
import com.google.gson.Gson
import io.reactivex.android.schedulers.AndroidSchedulers

@SuppressLint("CheckResult")
@Keep
class NetworkRepositoryImpl(private val apiService: ApiService, private val db: DataBaseRepositoryImpl) :
    NetworkRepository {
    override fun checkAds(responseListener: ResponseListener) {
        apiService.checkIpForAds()
            .subscribeOn(io.reactivex.schedulers.Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (it.body() is CheckAdsResponse) {
                    if (it.body()?.status!!) {
                        responseListener.onSuccess(it?.body()?.adProvider.toString())
                    } else {
                        responseListener.onFailure("failed check ads")
                    }
                } else {
                    responseListener.onFailure("failed check ads")
                }
            }, {
                it.printStackTrace()
                responseListener.onFailure("failed check ads")
            })
    }

    override fun refresh(token: String, responseListener: ResponseListener) {
        apiService.refresh(token)
            .subscribeOn(io.reactivex.schedulers.Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (it.body() is RefreshResponse) {
                    val user = db.getUser()
                    user?.let { usr ->
                        usr.token = it.body()!!.token
                        db.insertUser(usr)
                    }
                    responseListener.onSuccess("success refresh")
                } else {
                    responseListener.onFailure("failed refresh")
                    responseListener.onFailure(it.errorBody()?.string()!!.toString())
                }
            }, {
                it.printStackTrace()
                responseListener.onFailure("failed refresh")

            })
    }

    override fun ads(
        token: String,
        gid: String,
        hash: String,
        timeStamp: String,
        reward: String,
        responseListener: ResponseListener
    ) {
        apiService.ads("jwt $token", gid, hash, timeStamp, reward)
            .subscribeOn(io.reactivex.schedulers.Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (it.body() is AdsResponse) {
                    it.body()!!.money?.let { it1 -> db.updateBalance(it1) }
                    responseListener.onSuccess(it.body()!!.reward.toString())
                } else {
                    responseListener.onFailure("failed ads")
                    responseListener.onFailure(it.errorBody()?.string()!!.toString())
                }
            }, {
                it.printStackTrace()
                responseListener.onFailure("failed ads")

            })
    }

    override fun checkIP(responseListener: ResponseListener) {
        apiService.checkIpForAds()
            .subscribeOn(io.reactivex.schedulers.Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (it.body() is CheckAdsResponse) {
                    if (it.body()?.status!!) {
                        responseListener.onSuccess("true")
                    } else {
                        responseListener.onFailure("false")
                    }
                } else {
                    responseListener.onFailure("false")
                }
            }, {
                it.printStackTrace()
                responseListener.onFailure("false")
            })
    }

    override fun charge(token: String, phone: String, responseListener: ResponseListener) {
        apiService.charge("jwt $token", phone)
            .subscribeOn(io.reactivex.schedulers.Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (it.body() is ChargeResponse) {
                    if (it.body()!!.success == null) {
                        responseListener.onSuccess("null")
                    } else {
                        it.body()!!.money?.let { it1 -> db.updateBalance(it1) }

                        responseListener.onSuccess(it.body()!!.success.toString())
                    }
                } else {
                    try {
                        val gson = Gson()
                        val errorBody = gson.fromJson(it.errorBody()?.string().toString(), ChargeErrorBody::class.java)
                        if (errorBody.Errors!![0] == "not enough money in your account") {
                            responseListener.onFailure("اعتبار شما کافی نیست")
                        }
//                        responseListener.onFailure(errorBody.Errors!![0])
                    } catch (e: Exception) {
                        e.printStackTrace()
                        responseListener.onFailure("failed charge")
//                        responseListener.onFailure(it.errorBody()?.string()!!.toString())
                    }
                }
            }, {
                it.printStackTrace()
                responseListener.onFailure("failed charge")
            })
    }

    override fun me(token: String, responseListener: ResponseListener) {
        apiService.me("jwt $token")
            .subscribeOn(io.reactivex.schedulers.Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (it.body() is MeResponse) {
                    val user = db.getUser()
                    user?.let { usr ->
                        usr.apiKey = it.body()!!.apiKey
                        usr.balance = it.body()!!.money!!
                        db.insertUser(usr)
                    }
                    responseListener.onSuccess("success me")
                } else {
                    responseListener.onFailure("failed me")
                    responseListener.onFailure(it.errorBody()?.string()!!.toString())
                }
            }, {
                it.printStackTrace()
                responseListener.onFailure("failed me")

            })
    }

    override fun login(phone: String, code: String, responseListener: ResponseListener) {
        apiService.login(phone, code)
            .subscribeOn(io.reactivex.schedulers.Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (it.body() is LoginSuccess) {
                    AppManager.token = (it.body() as LoginSuccess).token.toString()

                    var user = db.getUser()
                    if (user == null) {
                        user = User(
                            userID = 1,
                            phone = (it.body() as LoginSuccess).user?.phone,
                            balance = (it.body() as LoginSuccess).user?.money!!,
                            token = (it.body() as LoginSuccess).token.toString()
                        )
                    }
                    db.insertUser(user)

                    responseListener.onSuccess("success login")
                } else {
                    responseListener.onFailure(it.errorBody()?.string()!!.toString())
                }
            }, {
                responseListener.onFailure("failed login")
                it.printStackTrace()
            })
    }

    override fun register(phone: String, responseListener: ResponseListener) {
        apiService.register(phone)
            .subscribeOn(io.reactivex.schedulers.Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (it.body()?.phone == phone) {
                    AppManager.phone = phone
                    responseListener.onSuccess("success register")
                } else {
                    responseListener.onFailure("failed register")
                }
            }, {
                responseListener.onFailure("failed register")
                it.printStackTrace()
            })
    }

}