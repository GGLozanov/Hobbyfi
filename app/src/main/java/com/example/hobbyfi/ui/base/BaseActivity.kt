package com.example.hobbyfi.ui.base

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.Navigation.findNavController
import com.example.hobbyfi.R
import com.example.hobbyfi.shared.PrefConfig
import kotlinx.android.synthetic.main.activity_chatroom.view.*
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

abstract class BaseActivity : AppCompatActivity(), KodeinAware {
    override val kodein: Kodein by kodein()
    protected lateinit var navController: NavController

    protected val prefConfig: PrefConfig by instance(tag = "prefConfig")

    override fun onStart() {
        super.onStart()
        navController = findNavController(this, R.id.nav_host_fragment)
    }

}