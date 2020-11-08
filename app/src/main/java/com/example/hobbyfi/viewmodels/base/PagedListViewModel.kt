package com.example.hobbyfi.viewmodels.base

import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.models.Model
import com.example.hobbyfi.persistence.BaseDao
import com.example.hobbyfi.state.State
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
abstract class PagedListViewModel<T: State, E: Intent, R: Model, N: BaseDao<R>>(application: MultiDexApplication) : StateIntentViewModel<T, E>(application) {

    // private val list: LiveData<PagedList<R>>
    // abstract dao init by Kodein?
}