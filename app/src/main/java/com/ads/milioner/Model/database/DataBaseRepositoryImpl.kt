package com.ads.milioner.Model.database

import androidx.lifecycle.LiveData
import com.ads.milioner.Model.database.model.User
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

class DataBaseRepositoryImpl(private val db: DataBase) : DataBaseRepository {
    override fun updateBalance(balance: Long) {
        GlobalScope.async {
            db.userDAO().updateBalance(balance)
        }
    }

    override fun clearData() {
        GlobalScope.async {
            db.userDAO().deleteAll()
        }
    }

    override fun getUser(): User? {
        return db.userDAO().getUser()
    }

    override fun getUserLiveData(): LiveData<User> {
        return db.userDAO().getUserLiveData()
    }

    override fun insertUser(user: User) {
        GlobalScope.async {
            db.userDAO().insert(user)
        }
    }


}