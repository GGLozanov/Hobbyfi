package com.example.hobbyfi.ui.base

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.example.hobbyfi.shared.Callbacks
import com.example.hobbyfi.shared.PrefConfig
import com.example.hobbyfi.utils.ImageUtils
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.kodein
import org.kodein.di.generic.instance

abstract class BaseFragment : Fragment(), KodeinAware {
    override val kodein: Kodein by kodein()

    protected val prefConfig: PrefConfig by instance(tag = "prefConfig")
        // no need for weakreference this time because PrefConfig will use appContext!

    protected lateinit var navController: NavController

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        super.onActivityCreated(savedInstanceState)
        navController = findNavController()
    }

}