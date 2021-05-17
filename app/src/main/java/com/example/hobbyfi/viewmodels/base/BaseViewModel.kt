package com.example.hobbyfi.viewmodels.base

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.repositories.Repository
import kotlinx.coroutines.CancellationException
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein

abstract class BaseViewModel(application: Application) : AndroidViewModel(application), KodeinAware {
    override val kodein: Kodein by kodein(application.applicationContext) // delegate Kodein instance through appcontext
}