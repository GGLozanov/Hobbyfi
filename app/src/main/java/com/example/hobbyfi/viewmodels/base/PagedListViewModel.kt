package com.example.hobbyfi.viewmodels.base

import androidx.multidex.MultiDexApplication
import androidx.paging.PagingSource
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.models.Model
import com.example.hobbyfi.persistence.BaseDao
import com.example.hobbyfi.state.State
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.kodein.di.generic.instance

@ExperimentalCoroutinesApi
abstract class PagedListViewModel<T: State, E: Intent, R: Model, N: BaseDao<R>>(application: MultiDexApplication) : StateIntentViewModel<T, E>(application) {
    // private val list: LiveData<PagedList<R>>
    // abstract dao init by Kodein?
}