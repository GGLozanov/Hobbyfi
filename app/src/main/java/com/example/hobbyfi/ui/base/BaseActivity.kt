package com.example.hobbyfi.ui.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.Navigation.findNavController
import com.example.hobbyfi.R
import kotlinx.android.synthetic.main.activity_chatroom.view.*
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein

abstract class BaseActivity : AppCompatActivity(), KodeinAware {
    override val kodein: Kodein by kodein()
    protected lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navController = findNavController(this, R.id.nav_host_fragment)
    }
}