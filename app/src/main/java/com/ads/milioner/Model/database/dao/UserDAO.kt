package com.ads.milioner.Model.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.ads.milioner.Model.database.model.User


@Dao
interface UserDAO {
    @Query("SELECT * from user where user_id=1 ")
    fun getUserLiveData(): LiveData<User>

    @Query("SELECT * from user where user_id=1 ")
    fun getUser(): User

    @Insert(onConflict = REPLACE)
    fun insert(user: User)

    @Delete
    fun delete(user: User)

    @Query("delete from user")
    fun deleteAll()

    @Update()
    fun update(user: User)
}