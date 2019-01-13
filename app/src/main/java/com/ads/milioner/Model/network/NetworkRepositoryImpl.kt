package com.ads.milioner.Model.network

import android.annotation.SuppressLint
import android.util.Log
import com.ads.milioner.Model.AppManager
import com.ads.milioner.Model.database.DataBaseRepositoryImpl
import com.ads.milioner.Model.database.model.User
import com.ads.milioner.Model.network.model.*
import io.reactivex.android.schedulers.AndroidSchedulers

@SuppressLint("CheckResult")
class NetworkRepositoryImpl(private val apiService: ApiService, private val db: DataBaseRepositoryImpl) :
    NetworkRepository {
    override fun ads(
        token: String,
        gid: String,
        hash: String,
        timeStamp: String,
        b: String,
        responseListener: ResponseListener
    ) {
        apiService.ads("jwt $token", gid, hash, timeStamp, b)
            .subscribeOn(io.reactivex.schedulers.Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (it.body() is AdsResponse) {
                    Log.d(AppManager.TAG, it.body()!!.reward.toString())
                    Log.d(AppManager.TAG, it.body()!!.money.toString())

                    val user = db.getUser()
                    if (user != null) {
                        user.balance = it.body()!!.money
                        db.insertUser(user)
                    }
                    responseListener.onFailure(it.body()!!.reward.toString())
                } else {
                    responseListener.onFailure("failed")
                    responseListener.onFailure(it.errorBody()?.string()!!.toString())
                }
            }, {
                it.printStackTrace()
                responseListener.onFailure("failed")

            })
    }

    override fun checkIP(responseListener: ResponseListener) {
        apiService.checkIP()
            .subscribeOn(io.reactivex.schedulers.Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (it.body() is CheckIPResponse) {
                    Log.d(AppManager.TAG, it.body()!!.status.toString())

                    responseListener.onSuccess(it.body()!!.status.toString())
                } else {
                    responseListener.onFailure("failed")
                    responseListener.onFailure(it.errorBody()?.string()!!.toString())
                }
            }, {
                it.printStackTrace()
                responseListener.onFailure("failed")

            })
    }

    override fun charge(token: String, responseListener: ResponseListener) {
        apiService.charge("jwt $token")
            .subscribeOn(io.reactivex.schedulers.Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (it.body() is ChargeResponse) {
                    Log.d(AppManager.TAG, it.body()!!.success.toString())
                    Log.d(AppManager.TAG, it.body()!!.money.toString())

                    val user = db.getUser()
                    if (user != null) {
                        user.balance = it.body()!!.money
                        db.insertUser(user)
                    }

                    responseListener.onSuccess(it.body()!!.success.toString())
                } else {
                    responseListener.onFailure("failed")
                    responseListener.onFailure(it.errorBody()?.string()!!.toString())
                }
            }, {
                it.printStackTrace()
                responseListener.onFailure("failed")

            })
    }

    override fun me(token: String, responseListener: ResponseListener) {
        apiService.me("jwt $token")
            .subscribeOn(io.reactivex.schedulers.Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (it.body() is MeResponse) {
                    Log.d(AppManager.TAG, it.body()!!.apiKey)
                    Log.d(AppManager.TAG, it.body()!!.phone)
                    Log.d(AppManager.TAG, it.body()!!.money.toString())
                    val user = db.getUser()
                    user?.let { usr ->
                        usr.apiKey = it.body()!!.apiKey
                        usr.balance = it.body()!!.money!!
                        db.insertUser(usr)
                    }
                    responseListener.onSuccess("success")
                } else {
                    responseListener.onFailure("failed")
                    responseListener.onFailure(it.errorBody()?.string()!!.toString())
                }
            }, {
                it.printStackTrace()
                responseListener.onFailure("failed")

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

                    responseListener.onSuccess("success")
                } else {
                    responseListener.onFailure(it.errorBody()?.string()!!.toString())
                }
            }, {
                responseListener.onFailure("failed")
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
                    responseListener.onSuccess("success")
                } else {
                    responseListener.onFailure("failed")
                }
            }, {
                responseListener.onFailure("failed")
                it.printStackTrace()
            })
    }

}