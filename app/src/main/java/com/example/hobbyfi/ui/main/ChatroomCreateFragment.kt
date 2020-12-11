package com.example.hobbyfi.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentChatroomCreateBinding
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.addTextChangedListener
import com.example.hobbyfi.ui.base.BaseFragment
import com.example.hobbyfi.ui.base.TextFieldInputValidationOnus
import com.example.hobbyfi.utils.FieldUtils
import com.example.hobbyfi.viewmodels.factories.TagListViewModelFactory
import com.example.hobbyfi.viewmodels.main.ChatroomCreateFragmentViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class ChatroomCreateFragment : MainFragment(), TextFieldInputValidationOnus {
    private val viewModel: ChatroomCreateFragmentViewModel by viewModels(factoryProducer = {
        TagListViewModelFactory(
            requireActivity().application,
            activityViewModel.authUser.value?.tags ?: ChatroomCreateFragmentArgs.fromBundle(
                requireActivity().intent?.extras!!
            )
                .user.tags ?: emptyList()
        )
    })
    private lateinit var binding: FragmentChatroomCreateBinding

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        (requireActivity() as MainActivity).bottom_nav.isVisible = false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentChatroomCreateBinding.inflate(inflater, container, false)

        initTextFieldValidators()

        with(binding) {

            return@onCreateView root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            buttonPair.leftButton.setOnClickListener { // tag select button

            }

            buttonPair.rightButton.setOnClickListener { // confirm button

            }
        }
    }

    override fun initTextFieldValidators() {
        with(binding) {
            // TODO: Fix code dup with other layouts like these and find a way to extract this in a single method call or something
            textInputName.addTextChangedListener(
                Constants.nameInputError,
                Constants.namePredicate
            )

            textInputDescription.addTextChangedListener(
                Constants.descriptionInputError,
                Constants.descriptionPredicate
            )
        }
    }

    override fun assertTextFieldsInvalidity(): Boolean {
        with(binding) {
            return@assertTextFieldsInvalidity FieldUtils.isTextFieldInvalid(textInputName) || FieldUtils.isTextFieldInvalid(textInputDescription)
        }
    }

    override fun onPause() {
        super.onPause()
        (requireActivity() as MainActivity).bottom_nav.isVisible = true
    }
}