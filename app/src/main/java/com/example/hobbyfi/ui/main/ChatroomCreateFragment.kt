package com.example.hobbyfi.ui.create

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.example.hobbyfi.R
import com.example.hobbyfi.ui.base.BaseFragment
import com.example.hobbyfi.ui.base.TextFieldInputValidationOnus
import com.example.hobbyfi.ui.main.MainActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

class ChatroomCreateFragment : BaseFragment(), TextFieldInputValidationOnus {

    @ExperimentalCoroutinesApi
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        (requireActivity() as MainActivity).bottom_nav.isVisible = false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chatroom_create, container, false)
    }

    override fun initTextFieldValidators() {
    }

    override fun assertTextFieldsInvalidity(): Boolean {
    }

    @ExperimentalCoroutinesApi
    override fun onPause() {
        super.onPause()
        (requireActivity() as MainActivity).bottom_nav.isVisible = true
    }
}