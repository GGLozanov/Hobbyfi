package com.example.hobbyfi.viewmodels.base

import android.app.Application
import androidx.databinding.Observable
import androidx.databinding.PropertyChangeRegistry
import androidx.multidex.MultiDexApplication
import androidx.paging.PagingSource
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.models.Message
import com.example.hobbyfi.models.Model
import com.example.hobbyfi.persistence.BaseDao
import com.example.hobbyfi.state.State
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
abstract class PagedListViewModel<T: State, E: Intent, R: Model, N: BaseDao<R>>(application: Application)
    : StateIntentViewModel<T, E>(application), Observable {
    protected abstract val pagingSource: PagingSource<Int, Message>

    @delegate:Transient
    private val callBacks: PropertyChangeRegistry by lazy { PropertyChangeRegistry() }

    override fun addOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) = callBacks.add(callback)

    override fun removeOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) = callBacks.remove(callback)

    // abstract dao init by Kodein?
}