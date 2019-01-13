package com.ads.milioner.Model.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class User(
    @PrimaryKey(autoGenerate = false) @ColumnInfo(name = "user_id") var userID: Int
    , @ColumnInfo(name = "phone") var phone: String? = ""
    , @ColumnInfo(name = "balance") var balance: Long?
    , @ColumnInfo(name = "api_key") var apiKey: String? = ""
    , @ColumnInfo(name = "token") var token: String? = ""
) {
    init {
        userID = 1
    }

}