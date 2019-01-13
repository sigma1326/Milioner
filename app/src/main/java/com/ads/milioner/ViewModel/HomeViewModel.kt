package com.ads.milioner.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.ads.milioner.Model.database.model.User

class HomeViewModel : ViewModel() {
    lateinit var user: LiveData<User>
}
