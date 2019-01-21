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
    override fun refresh(token: String, responseListener: ResponseListener) {
        apiService.refresh(token)
            .subscribeOn(io.reactivex.schedulers.Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (it.body() is RefreshResponse) {
                    Log.d(AppManager.TAG, it.body()!!.token)
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
        responseListener: ResponseListener
    ) {
        apiService.ads("jwt $token", gid, hash, timeStamp)
            .subscribeOn(io.reactivex.schedulers.Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (it.body() is AdsResponse) {
                    Log.d(AppManager.TAG, "reward: " + it.body()!!.reward.toString())
                    Log.d(AppManager.TAG, "money :" + it.body()!!.money.toString())

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
        apiService.checkIP()
            .subscribeOn(io.reactivex.schedulers.Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (it.body() is CheckIPResponse) {
                    Log.d(AppManager.TAG, it.body()!!.status.toString())

                    responseListener.onSuccess(it.body()!!.status.toString())
                } else {
                    responseListener.onFailure("failed check ip")
                    responseListener.onFailure(it.errorBody()?.string()!!.toString())
                }
            }, {
                it.printStackTrace()
                responseListener.onFailure("failed check ip")

            })
    }

    override fun charge(token: String, phone: String, responseListener: ResponseListener) {
        apiService.charge("jwt $token", phone)
            .subscribeOn(io.reactivex.schedulers.Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (it.body() is ChargeResponse) {
                    Log.d(AppManager.TAG, it.body()!!.success.toString())
                    Log.d(AppManager.TAG, it.body()!!.money.toString())

                    if (it.body()!!.success == null) {
                        responseListener.onSuccess("null")
                    } else {
                        it.body()!!.money?.let { it1 -> db.updateBalance(it1) }

                        responseListener.onSuccess(it.body()!!.success.toString())
                    }
                } else {
                    responseListener.onFailure("failed charge")
                    responseListener.onFailure(it.errorBody()?.string()!!.toString())
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
                    Log.d(AppManager.TAG, it.body()!!.apiKey)
                    Log.d(AppManager.TAG, it.body()!!.phone)
                    Log.d(AppManager.TAG, it.body()!!.money.toString())
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