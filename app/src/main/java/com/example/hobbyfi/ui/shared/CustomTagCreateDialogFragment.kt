package com.example.hobbyfi.ui.shared

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.util.Predicate
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.example.hobbyfi.R
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.ui.base.BaseDialogFragment
import com.example.hobbyfi.ui.base.TextFieldInputValidationOnus
import com.example.hobbyfi.utils.FieldUtils
import com.example.hobbyfi.viewmodels.shared.CustomTagCreateDialogFragmentViewModel
import com.example.spendidly.utils.PredicateTextWatcher
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import com.skydoves.colorpickerview.listeners.ColorListener
import kotlinx.android.synthetic.main.fragment_custom_tag_create_dialog.*

class CustomTagCreateDialogFragment : BaseDialogFragment(), TextFieldInputValidationOnus {

    private val viewModel: CustomTagCreateDialogFragmentViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        // TODO: Databinding inflate here

        color_picker.setLifecycleOwner(this)
        viewModel.setOnColourChangedListener(color_picker)

        cancel_button.setOnClickListener {
            dismiss()
        }

        confirm_custom_tag_button.setOnClickListener {
            if(FieldUtils.isTextFieldInvalid(text_input_tag_name)) {
                return@setOnClickListener
            }

            navController.previousBackStackEntry?.savedStateHandle?.set("tag", Tag(viewModel.name.value!!, viewModel.colour.value!!))
            dismiss()
        }

        return inflater.inflate(R.layout.fragment_custom_tag_create_dialog, container, false)
    }

    override fun initTextFieldValidators() {
        text_input_tag_name.addTextChangedListener(PredicateTextWatcher(
            text_input_tag_name,
            Constants.tagNameInputError,
            Predicate {
                return@Predicate it.isEmpty() || it.length > 25
            }
        ))
    }
}