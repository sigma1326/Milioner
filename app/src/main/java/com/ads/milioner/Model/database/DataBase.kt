package com.ads.milioner.Model.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ads.milioner.Model.database.dao.UserDAO
import com.ads.milioner.Model.database.model.User

@Database(entities = [(User::class)], version = 1, exportSchema = false)
abstract class DataBase : RoomDatabase() {

    abstract fun userDAO(): UserDAO
}