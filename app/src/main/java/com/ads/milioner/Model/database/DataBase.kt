package com.ads.milioner.Model.database

import androidx.annotation.Keep
import androidx.room.Database
import androidx.room.RoomDatabase
import com.ads.milioner.Model.database.dao.UserDAO
import com.ads.milioner.Model.database.model.User

@Database(entities = [(User::class)], version = 1, exportSchema = false)
@Keep
abstract class DataBase : RoomDatabase() {

    abstract fun userDAO(): UserDAO
}