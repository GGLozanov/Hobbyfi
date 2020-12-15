package com.example.hobbyfi.ui.chatroom

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentEventEditDialogBinding
import com.example.hobbyfi.ui.base.BaseDialogFragment
import com.example.hobbyfi.utils.FieldUtils
import com.example.hobbyfi.viewmodels.chatroom.EventEditDialogFragmentViewModel


class EventEditDialogFragment : ChatroomDialogFragment() {
    private lateinit var binding: FragmentEventEditDialogBinding
    private val viewModel: EventEditDialogFragmentViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        // TODO: Handle expired token error & logout
        binding = DataBindingUtil
            .inflate(layoutInflater, R.layout.fragment_event_edit_dialog, container, false)

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