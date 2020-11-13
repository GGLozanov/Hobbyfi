package com.example.hobbyfi.ui.shared

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.example.hobbyfi.R
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.ui.base.BaseDialogFragment
import com.example.hobbyfi.viewmodels.shared.CustomTagCreateDialogFragmentViewModel
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import com.skydoves.colorpickerview.listeners.ColorListener
import kotlinx.android.synthetic.main.fragment_custom_tag_create_dialog.*

class CustomTagCreateDialogFragment : BaseDialogFragment() {

    private val viewModel: CustomTagCreateDialogFragmentViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        color_picker.setLifecycleOwner(this)
        viewModel.setOnColourChangedListener(color_picker)

        cancel_button.setOnClickListener {

        }

        confirm_custom_tag_button.setOnClickListener {
            // TODO: Text field validation
            navController.previousBackStackEntry?.savedStateHandle?.set("tag", Tag(viewModel.name.value!!, viewModel.colour.value!!))
        }

        return inflater.inflate(R.layout.fragment_custom_tag_create_dialog, container, false)
    }
}