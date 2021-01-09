package com.example.hobbyfi.ui.chatroom

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentEventEditBinding
import com.example.hobbyfi.viewmodels.chatroom.EventEditFragmentViewModel


class EventEditFragment : ChatroomDialogFragment() {
    private lateinit var binding: FragmentEventEditBinding
    private val viewModel: EventEditFragmentViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        // TODO: Handle expired token error & logout
        binding = DataBindingUtil
            .inflate(layoutInflater, R.layout.fragment_event_edit, container, false)

        binding.viewModel = viewModel

        with(binding) {
            return@onCreateView root
        }
    }

    override fun initTextFieldValidators() {
    }

    override fun assertTextFieldsInvalidity(): Boolean {
        // return FieldUtils.isTextFieldInvalid()
        return true
    }
}