package com.example.hobbyfi.ui.main

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.navArgs
import com.example.hobbyfi.R
import com.example.hobbyfi.ui.base.BaseActivity

class MainActivity : BaseActivity() {
    // TODO: Viewpager 2 & bottomnav setup
    // TODO: Fetch viewmodel by delegation *everywhere*

    val args: MainActivityArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}