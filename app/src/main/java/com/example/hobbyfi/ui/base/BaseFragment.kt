package com.example.hobbyfi.ui.base

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.example.hobbyfi.shared.PrefConfig
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.kodein
import org.kodein.di.generic.instance

abstract class BaseFragment : Fragment(), KodeinAware {
    override val kodein: Kodein by kodein()

    protected val prefConfig: PrefConfig // no need for weakreference this time because PrefConfig will use appContext!
        get() {
            TODO()
            //instance<PrefConfig>()
        }

    protected lateinit var navController: NavController

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        navController = findNavController()
    }
}