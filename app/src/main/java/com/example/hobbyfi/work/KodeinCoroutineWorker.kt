package com.example.hobbyfi.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.shared.PrefConfig
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

abstract class KodeinCoroutineWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KodeinAware {
    override val kodein: Kodein by kodein(context)

    protected val prefConfig: PrefConfig by instance(tag = "prefConfig")

    protected val hobbyfiAPI: HobbyfiAPI by instance(tag = "api")
}