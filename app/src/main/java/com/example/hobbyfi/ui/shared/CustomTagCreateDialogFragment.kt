package com.example.hobbyfi.ui.shared

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.util.Predicate
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentCustomTagCreateDialogBinding
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.ui.base.BaseDialogFragment
import com.example.hobbyfi.ui.base.TextFieldInputValidationOnus
import com.example.hobbyfi.utils.FieldUtils
import com.example.hobbyfi.viewmodels.shared.CustomTagCreateDialogFragmentViewModel
import com.example.spendidly.utils.PredicateTextWatcher
import com.skydoves.colorpickerview.flag.BubbleFlag
import com.skydoves.colorpickerview.flag.FlagMode


class CustomTagCreateDialogFragment : BaseDialogFragment(), TextFieldInputValidationOnus {

    private val viewModel: CustomTagCreateDialogFragmentViewModel by viewModels()
    private lateinit var binding: FragmentCustomTagCreateDialogBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
         binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_custom_tag_create_dialog,
                container,
                false
            )

        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        val bubbleFlag = BubbleFlag(context)
        bubbleFlag.flagMode = FlagMode.FADE
        binding.colorPicker.flagView = bubbleFlag
        binding.colorPicker.setLifecycleOwner(this)
        viewModel.setOnColourChangedListener(binding.colorPicker)

        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        binding.confirmCustomTagButton.setOnClickListener {
            if(FieldUtils.isTextFieldInvalid(binding.textInputTagName)) {
                return@setOnClickListener
            }

            navController.previousBackStackEntry?.savedStateHandle?.set(
                Constants.tagsKey, Tag(
                    viewModel.name.value!!,
                    viewModel.colour.value!!
                )
            )
            dismiss()
        }

        return binding.root
    }

    override fun initTextFieldValidators() {
        binding.textInputTagName.addTextChangedListener(PredicateTextWatcher(
            binding.textInputTagName,
            Constants.tagNameInputError,
            Predicate {
                return@Predicate it.isEmpty() || it.length > 25
            }
        ))
    }
}