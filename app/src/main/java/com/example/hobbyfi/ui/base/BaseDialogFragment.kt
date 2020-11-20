package com.example.hobbyfi.ui.base

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.example.hobbyfi.shared.PrefConfig
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.kodein
import org.kodein.di.generic.instance

abstract class BaseDialogFragment : DialogFragment(), KodeinAware {
    override val kodein: Kodein by kodein()

    protected val prefConfig: PrefConfig by instance() // no need for weakreference this time because PrefConfig will use appContext!

    protected lateinit var navController: NavController

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        navController = findNavController()
    }
}