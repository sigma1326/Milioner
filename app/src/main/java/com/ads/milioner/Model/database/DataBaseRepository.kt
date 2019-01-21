package com.ads.milioner.Model.database

import androidx.annotation.Keep
import androidx.lifecycle.LiveData
import com.ads.milioner.Model.database.model.User

@Keep
interface DataBaseRepository {
    fun getUser(): User?
    fun getUserLiveData(): LiveData<User>
    fun insertUser(user: User)
    fun clearData()
    fun updateBalance(balance:Long)
}