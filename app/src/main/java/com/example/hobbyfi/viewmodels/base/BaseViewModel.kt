package com.example.hobbyfi.viewmodels.base

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.repositories.Repository

abstract class BaseViewModel(application: MultiDexApplication) : AndroidViewModel(application) {
    // TODO: Add abstract/default init method?
    // TODO: Add kodein init
}