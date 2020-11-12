package com.example.hobbyfi.viewmodels.base

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.repositories.Repository
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein

abstract class BaseViewModel(application: MultiDexApplication) : AndroidViewModel(application), KodeinAware {
    // TODO: Add abstract/default init method?
    override val kodein: Kodein by kodein(application.applicationContext) // delegate Kodein instance through appcontext
}